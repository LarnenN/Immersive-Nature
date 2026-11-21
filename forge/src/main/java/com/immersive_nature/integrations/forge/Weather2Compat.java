package com.immersive_nature.integrations.forge;

import com.immersive_nature.ImmersiveNature;
import com.immersive_nature.configs.CommonConfigs;
import com.immersive_nature.reg.ModBlocks;
import com.immersive_nature.util.NatureWeather;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Compat with Weather2 (Corosus). Weather2 storms rain locally while vanilla weather stays clear,
 * so Immersive Nature asks Weather2 where its storms are (by reflection, Weather2 is optional):
 * rain water, rain refill, moss and everything else that uses Level#isRainingAt then works under them.
 * Sandstorms pile up Immersive Nature sand piles instead of Weather2's own sand layer (see Weather2SandMixin).
 */
public final class Weather2Compat {
    private static final ResourceLocation W2_SAND_LAYER = new ResourceLocation("weather2", "sand_layer");

    private static boolean loaded;
    private static boolean broken;
    private static Method getManager;     // ServerTickHandler.getWeatherManagerFor(Level)
    private static Method getStorms;      // WeatherManager.getStormObjects()
    private static Class<?> stormClass;   // StormObject
    private static Method isPrecipitating;
    private static Field isDead, pos, size;

    /** Raining storms of one level as x, z, radius triples; refreshed once a second. */
    private static final class Storms {
        long time = -1000;
        double[] data = new double[0];
    }

    private static final Map<Level, Storms> STORMS = new WeakHashMap<>();
    private static final double[] EMPTY = new double[0];
    // last used level, so the map is not searched on every call (isRainingAt is called a lot)
    private static java.lang.ref.WeakReference<Level> lastLevel = new java.lang.ref.WeakReference<>(null);
    private static Storms lastStorms;

    private Weather2Compat() {
    }

    public static void init() {
        if (!ModList.get().isLoaded("weather2")) return;
        try {
            Class<?> handler = Class.forName("weather2.ServerTickHandler");
            getManager = handler.getMethod("getWeatherManagerFor", Level.class);
            getStorms = Class.forName("weather2.weathersystem.WeatherManager").getMethod("getStormObjects");
            stormClass = Class.forName("weather2.weathersystem.storm.StormObject");
            isPrecipitating = stormClass.getMethod("isPrecipitating");
            Class<?> weatherObject = Class.forName("weather2.weathersystem.storm.WeatherObject");
            isDead = weatherObject.getField("isDead");
            pos = weatherObject.getField("pos");
            size = weatherObject.getField("size");
            loaded = true;
            NatureWeather.extraRain = level -> level instanceof ServerLevel sl && stormData(sl).length > 0;
            NatureWeather.extraPrecipitationAt = (level, p) -> level instanceof ServerLevel sl && stormRainsAt(sl, p);
            ImmersiveNature.LOGGER.info("Weather2 found: its storms count as rain, sandstorms make Immersive Nature sand piles");
        } catch (Throwable t) {
            ImmersiveNature.LOGGER.warn("Weather2 compat disabled, unknown Weather2 version", t);
        }
    }

    private static double[] stormData(ServerLevel level) {
        if (!loaded || broken || !CommonConfigs.WEATHER2_RAIN.get()) return EMPTY;
        Storms s;
        synchronized (STORMS) {
            if (lastLevel.get() == level) {
                s = lastStorms;
            } else {
                s = STORMS.computeIfAbsent(level, l -> new Storms());
                lastLevel = new java.lang.ref.WeakReference<>(level);
                lastStorms = s;
            }
        }
        long time = level.getGameTime();
        if (time - s.time < 20 && time >= s.time) return s.data;
        double[] data = EMPTY;
        try {
            Object manager = getManager.invoke(null, level);
            if (manager != null) {
                List<?> storms = (List<?>) getStorms.invoke(manager);
                double[] tmp = new double[storms.size() * 3];
                int n = 0;
                for (Object o : storms) {
                    if (!stormClass.isInstance(o) || isDead.getBoolean(o) || !(Boolean) isPrecipitating.invoke(o)) continue;
                    Vec3 p = (Vec3) pos.get(o);
                    tmp[n++] = p.x;
                    tmp[n++] = p.z;
                    tmp[n++] = size.getInt(o);
                }
                data = java.util.Arrays.copyOf(tmp, n);
            }
        } catch (Throwable t) {
            broken = true;
            ImmersiveNature.LOGGER.warn("Weather2 compat stopped working", t);
        }
        s.data = data;
        s.time = time;
        return data;
    }

    /** True if a raining Weather2 storm is over this position (the usual sky and biome checks are done by the caller). */
    public static boolean stormRainsAt(ServerLevel level, BlockPos p) {
        if (!loaded) return false;
        double[] d = stormData(level);
        for (int i = 0; i < d.length; i += 3) {
            double dx = p.getX() + 0.5 - d[i], dz = p.getZ() + 0.5 - d[i + 1];
            if (dx * dx + dz * dz < d[i + 2] * d[i + 2]) return true;
        }
        return false;
    }

    /** Weather2's sand layer block -> Immersive Nature's sand pile (when enabled). */
    public static Block mapSandLayer(Block block) {
        if (block != null && CommonConfigs.WEATHER2_SAND.get() && W2_SAND_LAYER.equals(BuiltInRegistries.BLOCK.getKey(block))) {
            return ModBlocks.SAND_LAYER_BLOCK.get();
        }
        return block;
    }
}
