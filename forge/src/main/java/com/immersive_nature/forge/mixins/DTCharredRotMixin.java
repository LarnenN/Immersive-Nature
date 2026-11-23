package com.immersive_nature.forge.mixins;

import com.immersive_nature.integrations.DynamicTreesCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Charred Dynamic Trees branches (Immersive Nature's charred and smoldering charred families):
 * - have no leaves, so the tree would rot them away bit by bit; they stay as a burnt skeleton instead;
 * - don't burn again. A burnt branch removed by fire makes Dynamic Trees drop the whole tree above it,
 *   so a second burn would knock down the tree that just charred;
 * - smolder like the charred log: smoldering ones glow, give off embers and burn mobs that step on them,
 *   rain and water put them out, fire next to them lights them again (random ticks).
 * Other branches behave exactly as before. Skipped when Dynamic Trees is not installed.
 */
@Pseudo
@Mixin(targets = "com.ferreusveritas.dynamictrees.block.branch.BasicBranchBlock", remap = false)
public abstract class DTCharredRotMixin extends Block {

    protected DTCharredRotMixin(Properties properties) {
        super(properties);
    }

    @Inject(method = "checkForRot(Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;Lcom/ferreusveritas/dynamictrees/tree/species/Species;IILnet/minecraft/util/RandomSource;FZ)Z",
            at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void immersiveNature$charredNeverRots(CallbackInfoReturnable<Boolean> cir) {
        if (DynamicTreesCompat.isCharredBranch(this)) cir.setReturnValue(false);
    }

    @Inject(method = "getFlammability(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;)I",
            at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void immersiveNature$charredDoesNotBurn(CallbackInfoReturnable<Integer> cir) {
        if (DynamicTreesCompat.isCharredBranch(this)) cir.setReturnValue(0);
    }

    @Inject(method = "getFireSpreadSpeed(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;)I",
            at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void immersiveNature$charredDoesNotCatchFire(CallbackInfoReturnable<Integer> cir) {
        if (DynamicTreesCompat.isCharredBranch(this)) cir.setReturnValue(0);
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return DynamicTreesCompat.isCharredBranch(this) || super.isRandomlyTicking(state);
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (DynamicTreesCompat.isCharredBranch(this)) DynamicTreesCompat.charredBranchRandomTick(state, level, pos);
        else super.randomTick(state, level, pos, random);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        super.animateTick(state, level, pos, random);
        DynamicTreesCompat.smolderingBranchParticles(state, level, pos, random);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (DynamicTreesCompat.isSmolderingBranch(this) && !entity.fireImmune() && entity instanceof LivingEntity living
                && !EnchantmentHelper.hasFrostWalker(living)) {
            entity.hurt(entity.damageSources().hotFloor(), 1.0F);
        }
        super.stepOn(level, pos, state, entity);
    }

    @Override
    public int getLightEmission(BlockState state, BlockGetter level, BlockPos pos) {
        return DynamicTreesCompat.isSmolderingBranch(this) ? DynamicTreesCompat.SMOLDERING_LIGHT : super.getLightEmission(state, level, pos);
    }
}
