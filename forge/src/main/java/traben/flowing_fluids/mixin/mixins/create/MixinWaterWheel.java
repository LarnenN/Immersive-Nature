// Flowing Fluids by Traben (https://github.com/Traben-0/flowing_fluids), LGPL-3.0. Built into Immersive Nature.
package traben.flowing_fluids.mixin.mixins.create;

import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.content.kinetics.waterwheel.WaterWheelBlockEntity;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BubbleColumnBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import traben.flowing_fluids.FFFlowListenerLevel;
import traben.flowing_fluids.FFFluidUtils;
import traben.flowing_fluids.FlowingFluids;
import traben.flowing_fluids.IFFFlowListener;
import traben.flowing_fluids.config.FFConfig;

@Pseudo
@Mixin({WaterWheelBlockEntity.class})
public abstract class MixinWaterWheel extends GeneratingKineticBlockEntity implements IFFFlowListener {
   @Unique
   private BlockState setupState = null;
   @Unique
   private final Set<BlockPos> setupOffsets = new HashSet<>();
   @Unique
   private final Map<BlockPos, Pair<Vec3, Long>> recentDirections = new Object2ObjectOpenHashMap();

   @Shadow(
      remap = false
   )
   protected abstract Set<BlockPos> getOffsetsToCheck();

   @Shadow(
      remap = false
   )
   public abstract void setFlowScoreAndUpdate(int var1);

   @Shadow
   protected abstract Axis getAxis();

   public MixinWaterWheel(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
   }

   public void invalidate() {
      this.unMountListeners();
      super.invalidate();
   }

   @Unique
   private void mountListeners() {
      BlockState state = this.getBlockState();
      if (state != this.setupState) {
         this.setupState = state;
         if (!this.setupOffsets.isEmpty()) {
            this.unMountListeners();
         }

         if (this.level instanceof FFFlowListenerLevel listenerGetter) {
            Map<BlockPos, Set<BlockPos>> listeners = listenerGetter.ff$getFlowListenerPositions();

            for (BlockPos blockPos : this.getOffsetsToCheck()) {
               Set<BlockPos> list = listeners.computeIfAbsent(blockPos.offset(this.worldPosition), k -> new HashSet<>());
               list.add(this.worldPosition);
               this.setupOffsets.add(blockPos);
            }
         }
      }
   }

   @Unique
   private void unMountListeners() {
      if (this.level instanceof FFFlowListenerLevel listenerGetter) {
         Map<BlockPos, Set<BlockPos>> listeners = listenerGetter.ff$getFlowListenerPositions();

         for (BlockPos blockPos : this.setupOffsets) {
            BlockPos pos = blockPos.offset(this.worldPosition);
            Set<BlockPos> list = listeners.get(pos);
            if (list != null) {
               list.remove(this.worldPosition);
               if (list.isEmpty()) {
                  listeners.remove(pos);
               }
            }
         }

         this.setupOffsets.clear();
      }
   }

   @Inject(
      method = {"determineAndApplyFlowScore"},
      at = {@At("HEAD")},
      cancellable = true,
      remap = false
   )
   private void ff$modifyWaterCheck(CallbackInfo ci) {
      try {
         if (!FlowingFluids.config.enableMod
            || FlowingFluids.config.create_waterWheelMode == FFConfig.CreateWaterWheelMode.REQUIRE_FLOW
            || this.level == null) {
            return;
         }

         this.mountListeners();
         if (FlowingFluids.config.create_waterWheelMode.isRiver()
            && (
               !this.level.getBiome(this.worldPosition).is(BiomeTags.IS_RIVER)
                  || Math.abs(this.worldPosition.getY() - FFFluidUtils.seaLevel(this.level)) > 5
            )) {
            if (FlowingFluids.config.create_waterWheelMode.isRiverOnly()) {
               ci.cancel();
               this.setFlowScoreAndUpdate(0);
            }

            return;
         }

         ci.cancel();
         boolean lava = false;
         int score = 0;
         boolean fluidCanBeAnyHeight = !FlowingFluids.config.create_waterWheelMode.needsFullFluid();
         boolean oppositeSpin = FlowingFluids.config.create_waterWheelMode.isCounterSpin();
         boolean alwaysSpin = FlowingFluids.config.create_waterWheelMode.always();

         for (BlockPos blockPos : this.getOffsetsToCheck()) {
            BlockPos checkPos = blockPos.offset(this.worldPosition);
            FluidState fState = this.level.getFluidState(checkPos);
            lava |= fState.getType().isSame(Fluids.LAVA);
            if (alwaysSpin || !fState.isEmpty() && (fluidCanBeAnyHeight || fState.getAmount() == 8)) {
               score += oppositeSpin ? -1 : 1;
            }
         }

         if (score != 0 && !this.level.isClientSide()) {
            this.award(lava ? AllAdvancements.LAVA_WHEEL : AllAdvancements.WATER_WHEEL);
         }

         this.setFlowScoreAndUpdate(score);
      } catch (Exception var11) {
      }
   }

   @Override
   public void ff$acceptRecentFlow(BlockPos pos, Direction direction, Fluid fluid, boolean downAlso) {
      if (this.level != null && this.getOffsetsToCheck().contains(pos.subtract(this.worldPosition))) {
         if (fluid.isSame(Fluids.EMPTY)) {
            this.recentDirections.remove(pos);
         } else {
            this.recentDirections
               .put(
                  pos,
                  Pair.of(
                     new Vec3((double)direction.getStepX(), direction != Direction.DOWN && !downAlso ? 0.0 : -1.0, (double)direction.getStepZ()),
                     this.level.getGameTime()
                  )
               );
            if (!this.level.isClientSide()) {
               this.award(fluid.isSame(Fluids.LAVA) ? AllAdvancements.LAVA_WHEEL : AllAdvancements.WATER_WHEEL);
            }
         }
      }
   }

   @Inject(
      method = {"getFlowVectorAtPosition"},
      at = {@At("HEAD")},
      cancellable = true,
      remap = false
   )
   private void ff$modifyGetFlowVectorAtPosition(BlockPos pos, CallbackInfoReturnable<Vec3> cir) {
      if (this.level != null && FlowingFluids.config.enableMod && FlowingFluids.config.create_waterWheelMode.needsFlow()) {
         BlockState blockState = this.level.getBlockState(pos);
         if (blockState.getBlock() == Blocks.BUBBLE_COLUMN) {
            cir.setReturnValue(new Vec3(0.0, blockState.getValue(BubbleColumnBlock.DRAG_DOWN) ? -1.0 : 1.0, 0.0));
         } else {
            Pair<Vec3, Long> dir = this.recentDirections.get(pos);
            if (dir == null) {
               cir.setReturnValue(Vec3.ZERO);
            } else {
               boolean ignoreExpiry = FlowingFluids.config.dontTickAtLocation(this.worldPosition, this.level);
               if (!ignoreExpiry && (Long)dir.second() + (long)FlowingFluids.config.create_waterWheelFlowMaxTickInterval < this.level.getGameTime()) {
                  this.recentDirections.remove(pos);
                  cir.setReturnValue(Vec3.ZERO);
               } else {
                  cir.setReturnValue((Vec3)dir.first());
               }
            }
         }
      }
   }
}
