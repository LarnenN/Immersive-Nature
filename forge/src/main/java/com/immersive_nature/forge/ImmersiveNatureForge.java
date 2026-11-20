package com.immersive_nature.forge;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableBiMap;
import com.immersive_nature.ImmersiveNature;
import com.immersive_nature.reg.ModBlocks;
import com.immersive_nature.reg.ModWaxables;
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
@Mod(ImmersiveNature.MOD_ID)
public class ImmersiveNatureForge {
    public static final String MOD_ID = ImmersiveNature.MOD_ID;

    public ImmersiveNatureForge() {

        ImmersiveNature.commonInit();
        // snow (based on Snow! Real Magic) and fluids (based on Flowing Fluids)
        new com.immersive_nature.snow.util.CommonProxy();
        new com.immersive_nature.fluids.FlowingFluidsInit();
        com.immersive_nature.forge.overgrowth.Overgrowth.init();
        com.immersive_nature.forge.rainwater.RainWater.init();
        com.immersive_nature.forge.desirepaths.DesirePaths.init();

        // Dynamic Trees: seeds from leaf piles, soils, bark from stripped branches
        net.minecraftforge.registries.DeferredRegister<com.mojang.serialization.Codec<? extends net.minecraftforge.common.loot.IGlobalLootModifier>> lootModifiers =
                net.minecraftforge.registries.DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, MOD_ID);
        lootModifiers.register("dynamic_tree_seeds", DynamicSeedLootModifier.CODEC);
        lootModifiers.register(FMLJavaModLoadingContext.get().getModEventBus());
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, true, ImmersiveNatureForge::onRightClickBranch);
        if (net.minecraftforge.fml.ModList.get().isLoaded("dynamictrees")) {
            try {
                Class.forName("com.ferreusveritas.dynamictrees.api.registry.RegistryHandler").getMethod("setup", String.class).invoke(null, MOD_ID);
            } catch (Throwable e) {
                ImmersiveNature.LOGGER.error("Failed to register Immersive Nature soils with Dynamic Trees", e);
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
        var ret = com.immersive_nature.events.ModEvents.onBlockCLicked(event.getItemStack(),
                event.getEntity(), event.getLevel(), event.getHand(), event.getHitVec());
        if (ret != InteractionResult.PASS) {
            event.setCanceled(true);
            event.setCancellationResult(ret);
        }
    }


    private static void registerWaxables() {
        try {
            var oldWaxables = HoneycombItem.WAXABLES.get();
            HoneycombItem.WAXABLES = Suppliers.memoize(() -> ImmutableBiMap.<Block, Block>builder()
                    .putAll(oldWaxables)
                    .putAll(ModWaxables.getValues()).build());

            HoneycombItem.WAX_OFF_BY_BLOCK = Suppliers.memoize(() -> (HoneycombItem.WAXABLES.get()).inverse());

        } catch (Exception e) {
            ImmersiveNature.LOGGER.error("Failed to register Waxables: ", e);
        }

    }



    private static void onRightClickBranch(PlayerInteractEvent.RightClickBlock event) {
        var level = event.getLevel();
        var player = event.getEntity();
        var stack = event.getItemStack();
        if (level.isClientSide || !(stack.getItem() instanceof net.minecraft.world.item.AxeItem)) return;
        if (!com.immersive_nature.configs.CommonConfigs.AXE_STRIPPING.get()) return;
        if (player.isCreative() && !com.immersive_nature.configs.CommonConfigs.CREATIVE_DROP.get()) return;
        var pos = event.getPos();
        var state = level.getBlockState(pos);
        Item bark = com.immersive_nature.integrations.DynamicTreesCompat.getBranchBark(state, level, pos, player, stack);
        if (bark != null) {
            Block.popResourceFromFace(level, pos, event.getFace() == null ? net.minecraft.core.Direction.UP : event.getFace(), bark.getDefaultInstance());
        }
    }
}
