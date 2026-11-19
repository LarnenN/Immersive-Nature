package com.ordana.immersive_weathering.forge;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableBiMap;
import com.ordana.immersive_weathering.ImmersiveWeathering;
import com.ordana.immersive_weathering.reg.ModBlocks;
import com.ordana.immersive_weathering.reg.ModWaxables;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.HoneycombItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;

/**
 * Authors: MehVahdJukaar, Ordana, Keybounce,
 */
@Mod(ImmersiveWeathering.MOD_ID)
public class ImmersiveWeatheringForge {
    public static final String MOD_ID = ImmersiveWeathering.MOD_ID;

    public ImmersiveWeatheringForge() {

        ImmersiveWeathering.commonInit();
        com.ordana.immersive_weathering.forge.overgrowth.Overgrowth.init();

        // Dynamic Trees: seeds from leaf piles, soils, bark from stripped branches
        net.minecraftforge.registries.DeferredRegister<com.mojang.serialization.Codec<? extends net.minecraftforge.common.loot.IGlobalLootModifier>> lootModifiers =
                net.minecraftforge.registries.DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, MOD_ID);
        lootModifiers.register("dynamic_tree_seeds", DynamicSeedLootModifier.CODEC);
        lootModifiers.register(FMLJavaModLoadingContext.get().getModEventBus());
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, true, ImmersiveWeatheringForge::onRightClickBranch);
        if (net.minecraftforge.fml.ModList.get().isLoaded("dynamictrees")) {
            try {
                Class.forName("com.ferreusveritas.dynamictrees.api.registry.RegistryHandler").getMethod("setup", String.class).invoke(null, MOD_ID);
            } catch (Throwable e) {
                ImmersiveWeathering.LOGGER.error("Failed to register Immersive Nature soils with Dynamic Trees", e);
            }
        }

        // old worlds: blocks and items of Immersive Weathering keep existing under the new id
        MinecraftForge.EVENT_BUS.addListener(LegacyIdRemapper::onMissingMappings);

        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::registerOverrides);
        MinecraftForge.EVENT_BUS.register(this);


        /**
         * Update stuff:
         * Configs
         * sand later
         * ash layer
         * leaf layer
         */

        //TODO: fix layers texture generation
        //TODO: fix grass growth replacing double plants and add tag
    }

    public void registerOverrides(RegisterEvent event) {
        //override
        if (event.getRegistryKey() == ForgeRegistries.ITEMS.getRegistryKey())
            event.getForgeRegistry().register(new ResourceLocation("minecraft:hanging_roots"),
                    new CeilingAndWallBlockItem(Blocks.HANGING_ROOTS, ModBlocks.HANGING_ROOTS_WALL.get(),
                            new Item.Properties()));
    }


    @SubscribeEvent(priority = EventPriority.LOW)
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        var ret = com.ordana.immersive_weathering.events.ModEvents.onBlockCLicked(event.getItemStack(),
                event.getEntity(), event.getLevel(), event.getHand(), event.getHitVec());
        if (ret != InteractionResult.PASS) {
            event.setCanceled(true);
            event.setCancellationResult(ret);
        }
    }


    //TODO: add back on setup
    private static void registerWaxables() {
        try {
            var oldWaxables = HoneycombItem.WAXABLES.get();
            HoneycombItem.WAXABLES = Suppliers.memoize(() -> ImmutableBiMap.<Block, Block>builder()
                    .putAll(oldWaxables)
                    .putAll(ModWaxables.getValues()).build());

            HoneycombItem.WAX_OFF_BY_BLOCK = Suppliers.memoize(() -> (HoneycombItem.WAXABLES.get()).inverse());

        } catch (Exception e) {
            ImmersiveWeathering.LOGGER.error("Failed to register Waxables: ", e);
        }

    }



    private static void onRightClickBranch(PlayerInteractEvent.RightClickBlock event) {
        var level = event.getLevel();
        var player = event.getEntity();
        var stack = event.getItemStack();
        if (level.isClientSide || !(stack.getItem() instanceof net.minecraft.world.item.AxeItem)) return;
        if (!com.ordana.immersive_weathering.configs.CommonConfigs.AXE_STRIPPING.get()) return;
        if (player.isCreative() && !com.ordana.immersive_weathering.configs.CommonConfigs.CREATIVE_DROP.get()) return;
        var pos = event.getPos();
        var state = level.getBlockState(pos);
        Item bark = com.ordana.immersive_weathering.integration.DynamicTreesCompat.getBranchBark(state, level, pos, player, stack);
        if (bark != null) {
            Block.popResourceFromFace(level, pos, event.getFace() == null ? net.minecraft.core.Direction.UP : event.getFace(), bark.getDefaultInstance());
        }
    }
}
