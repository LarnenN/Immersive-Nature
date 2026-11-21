package com.immersive_nature.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public class SandLayerBlock extends LayerBlock {

    private final int particleColor;
    /** The full block a pile of 8 layers turns into (sand for sand piles); null = stays a pile. */
    @Nullable
    private final Supplier<Block> fullBlock;

    public SandLayerBlock(int particleColor, Properties properties) {
        this(particleColor, null, properties);
    }

    public SandLayerBlock(int particleColor, @Nullable Supplier<Block> fullBlock, Properties properties) {
        super(properties);
        this.particleColor = particleColor;
        this.fullBlock = fullBlock;
    }

    @Nullable
    public Block getFullBlock() {
        return fullBlock == null ? null : fullBlock.get();
    }

    /** 8 layers of sand are a full sand block: a pile that reaches 8 layers turns back into sand. */
    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        if (fullBlock != null && !level.isClientSide && getLayers(state) >= getMaxLayers()) {
            level.setBlock(pos, fullBlock.get().defaultBlockState(), 3);
            return;
        }
        super.onPlace(state, level, pos, oldState, isMoving);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (level.isClientSide && level.random.nextInt(8) == 0 && (entity.xOld != entity.getX() || entity.zOld != entity.getZ())) {
            addParticle(state, entity, pos, level, state.getValue(LAYERS_8), 0.05f);
        }
        super.stepOn(level, pos, state, entity);
    }

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, float height) {
        int layers = state.getValue(LAYERS_8);
        entity.causeFallDamage(height, layers > 2 ? 0.3f : 1, level.damageSources().fall());
        if (level.isClientSide) {
            for (int i = 0; i < Math.min(12, height * 1.4); i++) {

                addParticle(state, entity, pos, level, layers, 0.12f);
            }
        }
    }

    private void addParticle(BlockState state, Entity entity, BlockPos pos, Level level, int layers, float upSpeed) {
        level.addParticle(new BlockParticleOption(ParticleTypes.FALLING_DUST, state), entity.getX(), pos.getY() + layers * (1 / 8f), entity.getZ(),
                Mth.randomBetween(level.random, -1.0F, 1.0F) * 0.083333336F,
                upSpeed,
                Mth.randomBetween(level.random, -1.0F, 1.0F) * 0.083333336F);
    }

}
