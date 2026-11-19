package com.ordana.immersive_weathering.forge.desirepaths;

import com.ordana.immersive_weathering.ImmersiveWeathering;
import com.ordana.immersive_weathering.configs.CommonConfigs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Desire paths: grass under walking feet slowly wears down into dirt and coarse dirt.
 * Immersive Nature's own implementation of the idea of the "Desire Paths" mod by Insane96MCP.
 */
public final class DesirePaths {
    /** Plants that get trampled when the ground under them wears down. */
    public static final TagKey<Block> TRAMPLED_PLANTS = TagKey.create(Registries.BLOCK, ImmersiveWeathering.res("desire_path_trampled"));
    /** Mobs (besides players) that wear down paths too. Empty by default. */
    public static final TagKey<EntityType<?>> PATH_MOBS = TagKey.create(Registries.ENTITY_TYPE, ImmersiveWeathering.res("makes_desire_paths"));

    /** Normal walking moves about this many blocks per tick; used to scale the chance by speed. */
    private static final float WALK_SPEED = 0.12F;

    private static List<String> parsedFrom = null;
    private static List<String> parsedFromRef = null;
    private static List<Rule> rules = List.of();

    private record Rule(Block block, TagKey<Block> tag, Block result) {
        boolean matches(BlockState state) {
            return tag != null ? state.is(tag) : state.is(block);
        }
    }

    private DesirePaths() {
    }

    public static void init() {
        MinecraftForge.EVENT_BUS.addListener(DesirePaths::onPlayerTick);
        MinecraftForge.EVENT_BUS.addListener(DesirePaths::onLivingTick);
    }

    private static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        Player player = event.player;
        if (player.level().isClientSide || !player.onGround() || player.isSpectator()) return;
        if (CommonConfigs.DESIRE_PATHS_CROUCH_PREVENTS.get() && player.isCrouching()) return;
        wearDown(player);
    }

    private static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof Player || entity.level().isClientSide || !entity.onGround()) return;
        if (!entity.getType().is(PATH_MOBS)) return;
        wearDown(entity);
    }

    private static void wearDown(LivingEntity entity) {
        if (!CommonConfigs.DESIRE_PATHS.get()) return;
        List<Rule> rules = rules();
        if (rules.isEmpty()) return;

        float chance = CommonConfigs.DESIRE_PATHS_CHANCE.get().floatValue();
        if (CommonConfigs.DESIRE_PATHS_SPEED_BASED.get()) {
            chance *= (entity.walkDist - entity.walkDistO) / WALK_SPEED;
        }
        // standing still: nothing can wear down, skip the enchantment and block lookups
        if (chance <= 0) return;
        int featherFalling = EnchantmentHelper.getEnchantmentLevel(Enchantments.FALL_PROTECTION, entity);
        if (featherFalling > 0) {
            chance *= Math.max(0F, 1F - CommonConfigs.DESIRE_PATHS_FEATHER_FALLING.get().floatValue() * featherFalling);
        }
        if (chance <= 0) return;

        Level level = entity.level();
        AABB box = entity.getBoundingBox().deflate(0.1);
        int y = Mth.floor(entity.getY() - 1.0E-5);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        // every block column the entity is standing on
        for (int x = Mth.floor(box.minX); x < box.maxX; x++) {
            for (int z = Mth.floor(box.minZ); z < box.maxZ; z++) {
                pos.set(x, y, z);
                BlockState ground = level.getBlockState(pos);
                for (Rule rule : rules) {
                    if (!rule.matches(ground)) continue;
                    if (entity.getRandom().nextFloat() >= chance) break;
                    level.setBlockAndUpdate(pos, rule.result().defaultBlockState());
                    if (CommonConfigs.DESIRE_PATHS_BREAK_TALL_GRASS.get()) {
                        BlockPos above = pos.above();
                        if (level.getBlockState(above).is(TRAMPLED_PLANTS)) {
                            level.destroyBlock(above, false, entity);
                        }
                    }
                    break;
                }
            }
        }
    }

    /** Rules from the config, re-read whenever the config list changes. */
    private static List<Rule> rules() {
        List<String> lines = CommonConfigs.DESIRE_PATHS_TRANSFORMATIONS.get();
        // same list object = config not reloaded, skip the line-by-line compare
        if (lines != parsedFromRef && !Objects.equals(lines, parsedFrom)) {
            List<Rule> parsed = new ArrayList<>();
            for (String line : lines) {
                Rule rule = parse(line);
                if (rule != null) parsed.add(rule);
            }
            rules = List.copyOf(parsed);
            parsedFrom = List.copyOf(lines);
        }
        parsedFromRef = lines;
        return rules;
    }

    private static Rule parse(String line) {
        String[] parts = line.split(",");
        if (parts.length != 2) {
            ImmersiveWeathering.LOGGER.warn("Desire paths: ignoring \"{}\", expected \"from,to\"", line);
            return null;
        }
        String from = parts[0].trim();
        ResourceLocation toId = ResourceLocation.tryParse(parts[1].trim());
        Block to = toId == null ? null : BuiltInRegistries.BLOCK.getOptional(toId).orElse(null);
        if (to == null) {
            ImmersiveWeathering.LOGGER.warn("Desire paths: unknown block \"{}\" in \"{}\"", parts[1].trim(), line);
            return null;
        }
        if (from.startsWith("#")) {
            ResourceLocation tagId = ResourceLocation.tryParse(from.substring(1));
            if (tagId == null) return null;
            return new Rule(null, TagKey.create(Registries.BLOCK, tagId), to);
        }
        ResourceLocation fromId = ResourceLocation.tryParse(from);
        Block block = fromId == null ? null : BuiltInRegistries.BLOCK.getOptional(fromId).orElse(null);
        if (block == null) {
            ImmersiveWeathering.LOGGER.warn("Desire paths: unknown block \"{}\" in \"{}\"", from, line);
            return null;
        }
        return new Rule(block, null, to);
    }
}
