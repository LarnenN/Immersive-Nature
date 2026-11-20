package com.immersive_nature.util;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.LegacyRandomSource;

import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Immersive Nature: allocation-free area scans.
 * <p>
 * Immersive Weathering used to build a fresh list of every position in the area, shuffle it with a new
 * Random and throw it all away on every check. Here the offsets of each area size are computed once
 * (same order as {@link BlockPos#withinManhattan}), and the visiting order is shuffled with the exact same
 * algorithm and seed as {@code Collections.shuffle(list, new Random(Mth.getSeed(center)))}, using reusable
 * per-thread buffers. Same positions, same order, same results, no garbage.
 */
public final class AreaTemplates {

    @FunctionalInterface
    public interface Visitor {
        /** @return true to stop visiting */
        boolean visit(BlockPos.MutableBlockPos pos);
    }

    /** Offsets packed as dx, dy, dz triples, in withinManhattan order. */
    private static final ConcurrentHashMap<Long, int[]> TEMPLATES = new ConcurrentHashMap<>();

    private static final class Scratch {
        final Random random = new Random();
        final LegacyRandomSource randomSource = new LegacyRandomSource(0);
        int[][] orders = new int[4][];
        BlockPos.MutableBlockPos[] cursors = new BlockPos.MutableBlockPos[4];
        int depth = 0;
    }

    private static final ThreadLocal<Scratch> SCRATCH = ThreadLocal.withInitial(Scratch::new);

    private AreaTemplates() {
    }

    /** Fast lookup for the usual small radii (0-15), no boxing. */
    private static final int[][] SMALL = new int[16 * 16 * 16][];

    private static int[] template(int rx, int ry, int rz) {
        if ((rx | ry | rz) >= 0 && rx < 16 && ry < 16 && rz < 16) {
            int idx = (rx << 8) | (ry << 4) | rz;
            int[] t = SMALL[idx];
            if (t == null) SMALL[idx] = t = build(rx, ry, rz);
            return t;
        }
        long key = ((long) (rx & 0xFFFF) << 32) | ((long) (ry & 0xFFFF) << 16) | (rz & 0xFFFF);
        return TEMPLATES.computeIfAbsent(key, k -> build(rx, ry, rz));
    }

    private static int[] build(int rx, int ry, int rz) {
        {
            int count = 0;
            for (BlockPos ignored : BlockPos.withinManhattan(BlockPos.ZERO, rx, ry, rz)) count++;
            int[] offsets = new int[count * 3];
            int i = 0;
            for (BlockPos p : BlockPos.withinManhattan(BlockPos.ZERO, rx, ry, rz)) {
                offsets[i++] = p.getX();
                offsets[i++] = p.getY();
                offsets[i++] = p.getZ();
            }
            return offsets;
        }
    }

    /**
     * Visits the positions around {@code center} in the same order as the old
     * {@code grabBlocksAroundRandomly}. The position passed to the visitor is reused: don't keep it.
     *
     * @return true if the visitor stopped early
     */
    public static boolean forEachRandomly(BlockPos center, int rx, int ry, int rz, Visitor visitor) {
        int[] offsets = template(rx, ry, rz);
        int n = offsets.length / 3;
        Scratch s = SCRATCH.get();
        int depth = s.depth++;
        try {
            if (depth >= s.orders.length) {
                s.orders = java.util.Arrays.copyOf(s.orders, depth * 2);
                s.cursors = java.util.Arrays.copyOf(s.cursors, depth * 2);
            }
            int[] order = s.orders[depth];
            if (order == null || order.length < n) order = s.orders[depth] = new int[Math.max(n, 16)];
            BlockPos.MutableBlockPos cursor = s.cursors[depth];
            if (cursor == null) cursor = s.cursors[depth] = new BlockPos.MutableBlockPos();

            for (int i = 0; i < n; i++) order[i] = i;
            // exactly what Collections.shuffle does for a RandomAccess list
            Random rnd = s.random;
            rnd.setSeed(Mth.getSeed(center));
            for (int i = n; i > 1; i--) {
                int j = rnd.nextInt(i);
                int t = order[i - 1];
                order[i - 1] = order[j];
                order[j] = t;
            }

            int cx = center.getX(), cy = center.getY(), cz = center.getZ();
            for (int i = 0; i < n; i++) {
                int o = order[i] * 3;
                cursor.set(cx + offsets[o], cy + offsets[o + 1], cz + offsets[o + 2]);
                if (visitor.visit(cursor)) return true;
            }
            return false;
        } finally {
            s.depth--;
        }
    }

    /** A per-thread Random seeded like {@code new Random(seed)}. Only use it before the next call on this thread. */
    public static Random random(long seed) {
        Random r = SCRATCH.get().random;
        r.setSeed(seed);
        return r;
    }

    /** A per-thread RandomSource seeded like {@code RandomSource.create(seed)}. Only use it before the next call on this thread. */
    public static LegacyRandomSource randomSource(long seed) {
        LegacyRandomSource r = SCRATCH.get().randomSource;
        r.setSeed(seed);
        return r;
    }
}
