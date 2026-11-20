package com.immersive_nature.reg;

import com.immersive_nature.ImmersiveWeathering;
import com.immersive_nature.entities.FallingIcicleEntity;
import com.immersive_nature.entities.FallingLayerEntity;
import com.immersive_nature.entities.FallingPropaguleEntity;
import com.immersive_nature.entities.IcicleBlockEntity;
import net.mehvahdjukaar.moonlight.api.platform.PlatHelper;
import net.mehvahdjukaar.moonlight.api.platform.RegHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.function.Supplier;

public class ModEntities {

    public static void init() {
    }

    //entities
    public static Supplier<EntityType<FallingIcicleEntity>> FALLING_ICICLE = RegHelper.registerEntityType(
            ImmersiveWeathering.res("falling_icicle"),
            FallingIcicleEntity::new, MobCategory.MISC, 0.98F, 0.98F, 10, 20);

    public static Supplier<EntityType<FallingLayerEntity>> FALLING_LAYER = RegHelper.registerEntityType(
            ImmersiveWeathering.res("falling_layer"),
            FallingLayerEntity::new, MobCategory.MISC, 0.98F, 0.98F, 10, 20);

    public static Supplier<EntityType<FallingPropaguleEntity>> FALLING_PROPAGULE = RegHelper.registerEntityType(
            ImmersiveWeathering.res("falling_propagule"),
            FallingPropaguleEntity::new, MobCategory.MISC, 0.28F, 0.98F, 10, 20);


    //block entities

    public static final Supplier<BlockEntityType<IcicleBlockEntity>> ICICLE_TILE = RegHelper.registerBlockEntityType(
            ImmersiveWeathering.res("icicle"),
            () -> PlatHelper.newBlockEntityType(IcicleBlockEntity::new, ModBlocks.ICICLE.get()));


}
