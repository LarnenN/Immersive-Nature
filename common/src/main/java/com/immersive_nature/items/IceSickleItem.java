package com.immersive_nature.items;

import com.immersive_nature.configs.CommonConfigs;
import com.immersive_nature.reg.ModFoods;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class IceSickleItem extends SwordItem {

    public IceSickleItem(Tier tier, int i, float v, Properties properties) {
        super(tier, i, v, properties);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        entity.setTicksFrozen(Math.min(entity.getTicksRequiredToFreeze(), entity.getTicksFrozen() + 80));
        return super.finishUsingItem(stack, level, entity);
    }

    @Nullable
    @Override
    public FoodProperties getFoodProperties() {
        return CommonConfigs.ICICLE_FOOD.get() ? ModFoods.ICICLE : null;
    }
    @Override
    public boolean isEdible() {
        return CommonConfigs.ICICLE_FOOD.get();
    }
}
