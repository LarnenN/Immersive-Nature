package com.ordana.immersive_weathering.entities;

import com.ordana.immersive_weathering.reg.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import org.jetbrains.annotations.Nullable;

public class FallingIcicleEntity extends FallingBlockEntity {

    public FallingIcicleEntity(EntityType<? extends FallingBlockEntity> type, Level level) {
        super(type, level);
    }

    public FallingIcicleEntity(Level level, double x, double y, double z, BlockState state) {
        this(ModEntities.FALLING_ICICLE.get(), level);
        this.setBlockState(state);
        this.blocksBuilding = true;
        this.setPos(x, y, z);
        this.setDeltaMovement(Vec3.ZERO);
        this.xo = x;
        this.yo = y;
        this.zo = z;
        this.setStartPos(this.blockPosition());
    }

    //workaround
    public void setBlockState(BlockState state) {
        if (state.hasProperty(BlockStateProperties.WATERLOGGED)) {
            state = state.setValue(BlockStateProperties.WATERLOGGED, false);
        }
        CompoundTag tag = new CompoundTag();
        tag.put("BlockState", NbtUtils.writeBlockState(state));
        tag.putInt("Time", this.time);
        this.readAdditionalSaveData(tag);
    }

    public static FallingBlockEntity fall(Level level, BlockPos pos, BlockState state) {
        FallingBlockEntity fallingblockentity = new FallingIcicleEntity(level,
                pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D,
                state.hasProperty(BlockStateProperties.WATERLOGGED) ? state.setValue(BlockStateProperties.WATERLOGGED, false) : state);
        level.setBlock(pos, state.getFluidState().createLegacyBlock(), 3);
        level.addFreshEntity(fallingblockentity);
        return fallingblockentity;
    }

    @Nullable
    @Override
    public ItemEntity spawnAtLocation(ItemLike itemLike) {
        return null;
    }

    /** Ticks the icicle has not moved down; used to catch icicles that got stuck. */
    private int stuckTicks = 0;

    @Override
    public void tick() {
        if (!this.level().isClientSide && !this.isRemoved()) {
            // a falling icicle never sticks anywhere: it shatters as soon as it lands,
            // gets stuck on something, or after falling for too long
            if (this.onGround() || this.isInWater() || this.isInLava()) {
                this.shatter();
                return;
            }
            if (this.time > 2 && Math.abs(this.getY() - this.yo) < 1.0E-3) {
                if (++this.stuckTicks >= 3) {
                    this.shatter();
                    return;
                }
            } else {
                this.stuckTicks = 0;
            }
            if (this.time > 600) {
                this.shatter();
                return;
            }
        }
        super.tick();
    }

    /** Breaks the icicle with the sound of breaking ice and shards of ice. */
    public void shatter() {
        if (this.level() instanceof ServerLevel level) {
            BlockState state = this.getBlockState();
            double y = this.getY() + 0.3;
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state),
                    this.getX(), y, this.getZ(), 24, 0.25, 0.3, 0.25, 0.15);
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, net.minecraft.world.level.block.Blocks.ICE.defaultBlockState()),
                    this.getX(), y, this.getZ(), 12, 0.3, 0.2, 0.3, 0.2);
            if (!this.isSilent()) {
                level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.GLASS_BREAK, SoundSource.BLOCKS,
                        1.0F, 0.9F + level.random.nextFloat() * 0.3F);
            }
        }
        this.discard();
    }
}
