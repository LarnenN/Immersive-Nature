package com.immersive_nature.forge.desirepaths;

import com.immersive_nature.ImmersiveNature;
import com.immersive_nature.blocks.LayerBlock;
import com.immersive_nature.blocks.SandLayerBlock;
import com.immersive_nature.reg.ModBlocks;
import com.immersive_nature.configs.CommonConfigs;
import com.immersive_nature.util.BlockRules;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;

import it.unimi.dsi.fastutil.longs.Long2FloatOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.List;
import org.jetbrains.annotations.Nullable;

/**
 * Desire paths: grass and podzol under walking feet slowly wear down into dirt and coarse dirt,
 * and sand piles (sand layers) get pressed down one layer at a time.
 * Each block remembers how much it was walked on (not which mob did it).
 * Immersive Nature's own implementation of the idea of the "Desire Paths" mod by Insane96MCP.
 */
public final class DesirePaths {
    /** Plants that get trampled when the ground under them wears down. */
    public static final TagKey<Block> TRAMPLED_PLANTS = TagKey.create(Registries.BLOCK, ImmersiveNature.res("desire_path_trampled"));
    /** Mobs that wear down paths when mobs_make_paths is off. Empty by default. */
    public static final TagKey<EntityType<?>> PATH_MOBS = TagKey.create(Registries.ENTITY_TYPE, ImmersiveNature.res("makes_desire_paths"));
    /** Mobs that never wear down paths when mobs_make_paths is on (sheep by default). */
    public static final TagKey<EntityType<?>> NO_PATH_MOBS = TagKey.create(Registries.ENTITY_TYPE, ImmersiveNature.res("no_desire_paths"));

    /** Normal walking moves about this many blocks per tick; used to scale the chance by speed. */
    private static final float WALK_SPEED = 0.12F;

    private static final BlockRules RULES = new BlockRules("Desire paths", () -> CommonConfigs.DESIRE_PATHS_TRANSFORMATIONS.get(), 1);

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
        if (CommonConfigs.DESIRE_PATHS_MOBS.get()) {
            if (entity.getType().is(NO_PATH_MOBS)) return;
        } else if (!entity.getType().is(PATH_MOBS)) return;
        wearDown(entity);
    }

    /**
     * Wear remembered per block (not per mob): every tick someone walks on a block adds to its wear;
     * when it reaches the configured amount the block wears down one step and starts again.
     */
    private static final class Wear {
        final Long2FloatOpenHashMap amount = new Long2FloatOpenHashMap();
        final Long2LongOpenHashMap lastTouched = new Long2LongOpenHashMap();
        long nextCleanup = 0;
    }

    private static final Map<Level, Wear> WEAR = new WeakHashMap<>();

    private static Wear wear(Level level) {
        Wear w = WEAR.computeIfAbsent(level, l -> new Wear());
        long time = level.getGameTime();
        if (time >= w.nextCleanup) {
            // forget blocks nobody walked on for a while, so the memory never grows without end
            long forget = CommonConfigs.DESIRE_PATHS_FORGET.get() * 1200L;
            var it = w.lastTouched.long2LongEntrySet().fastIterator();
            while (it.hasNext()) {
                var e = it.next();
                if (time - e.getLongValue() > forget) {
                    w.amount.remove(e.getLongKey());
                    it.remove();
                }
            }
            w.nextCleanup = time + 1200;
        }
        return w;
    }

    /**
     * What a sand block or sand pile becomes when it is walked down one step, or null if it can't be.
     * Full sand counts as 8 layers: sand -> pile of 7 -> 6 ... -> 1 -> gone (or stops at sand_min_layers).
     */
    @Nullable
    private static BlockState pressSand(BlockState ground) {
        Block block = ground.getBlock();
        int layers;
        SandLayerBlock pile;
        if (block == ModBlocks.SAND_LAYER_BLOCK.get() || block == ModBlocks.RED_SAND_LAYER_BLOCK.get()) {
            pile = (SandLayerBlock) block;
            layers = ground.getValue(LayerBlock.LAYERS_8);
        } else if (block == Blocks.SAND && CommonConfigs.DESIRE_PATHS_SAND_BLOCKS.get()) {
            pile = (SandLayerBlock) ModBlocks.SAND_LAYER_BLOCK.get();
            layers = 8;
        } else if (block == Blocks.RED_SAND && CommonConfigs.DESIRE_PATHS_SAND_BLOCKS.get()) {
            pile = (SandLayerBlock) ModBlocks.RED_SAND_LAYER_BLOCK.get();
            layers = 8;
        } else {
            return null;
        }
        int min = CommonConfigs.DESIRE_PATHS_SAND_MIN.get();
        if (min > 0 && layers <= min) return null;
        if (layers <= 1) return Blocks.AIR.defaultBlockState();
        return pile.defaultBlockState().setValue(LayerBlock.LAYERS_8, layers - 1);
    }

    /** Grassy silt -> silt and grassy sandy dirt -> sandy dirt; the plain soils don't wear any further. */
    @Nullable
    private static Block wornSoil(Block block) {
        if (block == ModBlocks.GRASSY_SILT.get()) return ModBlocks.SILT.get();
        if (block == ModBlocks.GRASSY_SANDY_DIRT.get()) return ModBlocks.SANDY_DIRT.get();
        return null;
    }

    private static void trample(Level level, BlockPos pos, LivingEntity entity) {
        if (CommonConfigs.DESIRE_PATHS_BREAK_TALL_GRASS.get()) {
            BlockPos above = pos.above();
            if (level.getBlockState(above).is(TRAMPLED_PLANTS)) {
                level.destroyBlock(above, false, entity);
            }
        }
    }

    private static void wearDown(LivingEntity entity) {
        if (!CommonConfigs.DESIRE_PATHS.get()) return;
        // standing still wears nothing
        float moved = entity.walkDist - entity.walkDistO;
        if (moved <= 0) return;
        List<BlockRules.Rule> rules = RULES.get();
        boolean sand = CommonConfigs.DESIRE_PATHS_SAND.get();
        boolean soils = CommonConfigs.DESIRE_PATHS_SOILS.get();
        if (rules.isEmpty() && !sand && !soils) return;

        float add = CommonConfigs.DESIRE_PATHS_SPEED_BASED.get() ? moved / WALK_SPEED : 1F;
        int featherFalling = EnchantmentHelper.getEnchantmentLevel(Enchantments.FALL_PROTECTION, entity);
        if (featherFalling > 0) {
            add *= Math.max(0F, 1F - CommonConfigs.DESIRE_PATHS_FEATHER_FALLING.get().floatValue() * featherFalling);
        }
        if (add <= 0) return;

        Level level = entity.level();
        int needed = CommonConfigs.DESIRE_PATHS_WEAR.get();
        Wear wear = null;
        AABB box = entity.getBoundingBox().deflate(0.1);
        int y = Mth.floor(entity.getY() - 1.0E-5);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        // every block column the entity is standing on
        for (int x = Mth.floor(box.minX); x < box.maxX; x++) {
            for (int z = Mth.floor(box.minZ); z < box.maxZ; z++) {
                pos.set(x, y, z);
                BlockState ground = level.getBlockState(pos);
                if (sand) {
                    BlockState pressed = pressSand(ground);
                    if (pressed != null) {
                        if (wear == null) wear = wear(level);
                        long key = pos.asLong();
                        wear.lastTouched.put(key, level.getGameTime());
                        if (wear.amount.addTo(key, add) + add < CommonConfigs.DESIRE_PATHS_SAND_WEAR.get()) continue;
                        wear.amount.remove(key);
                        wear.lastTouched.remove(key);
                        level.setBlockAndUpdate(pos, pressed);
                        continue;
                    }
                }
                if (soils) {
                    Block worn = wornSoil(ground.getBlock());
                    if (worn != null) {
                        if (wear == null) wear = wear(level);
                        long key = pos.asLong();
                        wear.lastTouched.put(key, level.getGameTime());
                        if (wear.amount.addTo(key, add) + add < needed) continue;
                        wear.amount.remove(key);
                        wear.lastTouched.remove(key);
                        level.setBlockAndUpdate(pos, worn.defaultBlockState());
                        trample(level, pos, entity);
                        continue;
                    }
                }
                for (BlockRules.Rule rule : rules) {
                    if (!rule.matches(ground)) continue;
                    if (wear == null) wear = wear(level);
                    long key = pos.asLong();
                    wear.lastTouched.put(key, level.getGameTime());
                    if (wear.amount.addTo(key, add) + add < needed) break;
                    wear.amount.remove(key);
                    wear.lastTouched.remove(key);
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
}
