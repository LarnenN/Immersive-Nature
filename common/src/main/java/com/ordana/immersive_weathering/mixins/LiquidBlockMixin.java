package com.ordana.immersive_weathering.mixins;

import com.google.common.collect.ImmutableList;
import com.ordana.immersive_weathering.IWPlatformStuff;
import com.ordana.immersive_weathering.data.fluid_generators.FluidGeneratorsHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.ordana.immersive_weathering.erosion.Tasks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;

@Mixin(LiquidBlock.class)
public abstract class LiquidBlockMixin extends Block implements BucketPickup {

    @Shadow
    @Final
    public static ImmutableList<Direction> POSSIBLE_FLOW_DIRECTIONS;

    @Shadow
    protected abstract void fizz(LevelAccessor levelAccessor, BlockPos pos);

    @Shadow
    public abstract FluidState getFluidState(BlockState blockState);

    @Shadow
    @Final
    protected FlowingFluid fluid;

    protected LiquidBlockMixin(Properties settings, FlowingFluid fluid) {
        super(settings);
    }

    @Inject(method = "shouldSpreadLiquid", at = @At("HEAD"), cancellable = true)
    private void shouldSpreadLiquid(Level level, BlockPos pos, BlockState state, CallbackInfoReturnable<Boolean> cir) {
        var f = this.getOwnFluid();
        var successPos = FluidGeneratorsHandler.applyGenerators(f, POSSIBLE_FLOW_DIRECTIONS, pos, level);
        if(successPos.isPresent()){
            if(f.is(FluidTags.LAVA)) {
                this.fizz(level, successPos.get().getFirst());
            }
            cir.setReturnValue(false);
        }
    }

    // Water Erosion: water blocks receive random ticks that erode the terrain around them
    @Inject(method = "isRandomlyTicking", at = @At("HEAD"), cancellable = true)
    private void immersiveNature$waterTicks(BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (state.getBlock() == Blocks.WATER) cir.setReturnValue(true);
    }

    @Inject(method = "randomTick", at = @At("HEAD"))
    private void immersiveNature$erode(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        if (state.getBlock() == Blocks.WATER) Tasks.INSTANCE.run(level, state, pos, random);
    }

    @Unique
    public FlowingFluid getOwnFluid() {
        var f = IWPlatformStuff.getFlowingFluid((LiquidBlock) (Object) this);
        if (f == null)
            return this.fluid;
        return f;
    }

}
