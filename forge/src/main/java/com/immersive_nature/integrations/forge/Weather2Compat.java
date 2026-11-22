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
    private static Field isDead, pos, size, stage;
    private static Class<?> particleStormClass; // WeatherObjectParticleStorm (sandstorm / snowstorm)
    private static Method getType, getSize;

    /** Raining storms of one level as x, z, radius triples; refreshed once a second. */
    private static final class Storms {
        long time = -1000;
        double[] data = new double[0];
        /** Intensity stage of each raining storm in data (0 rain, 1 thunder, 2 high wind, 3 hail, 4+ forming / tornado). */
        int[] stages = new int[0];
        double[] sand = new double[0];
        double[] snow = new double[0];
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
            try {
                stage = stormClass.getField("levelCurIntensityStage");
            } catch (Throwable t) {
                stage = null;
            }
            Class<?> weatherObject = Class.forName("weather2.weathersystem.storm.WeatherObject");
            isDead = weatherObject.getField("isDead");
            pos = weatherObject.getField("pos");
            size = weatherObject.getField("size");
            try {
                particleStormClass = Class.forName("weather2.weathersystem.storm.WeatherObjectParticleStorm");
                getType = particleStormClass.getMethod("getType");
                getSize = particleStormClass.getMethod("getSize");
            } catch (Throwable t) {
                particleStormClass = null;
            }
            loaded = true;
            NatureWeather.extraRain = level -> level instanceof ServerLevel sl && (stormData(sl).length > 0 || storms(sl).sand.length > 0 || storms(sl).snow.length > 0);
            NatureWeather.extraPrecipitationAt = (level, p) -> level instanceof ServerLevel sl && stormRainsAt(sl, p);
            NatureWeather.extraRainWaterAt = (level, p) -> level instanceof ServerLevel sl ? rainWaterAt(sl, p) : -1;
            NatureWeather.maxExtraRainWater = () -> !CommonConfigs.WEATHER2_RAIN.get() ? 0 : Math.max(Math.max(CommonConfigs.WEATHER2_WATER_RAIN.get(), CommonConfigs.WEATHER2_WATER_LIGHTNING.get()),
                    Math.max(Math.max(CommonConfigs.WEATHER2_WATER_HIGHWIND.get(), CommonConfigs.WEATHER2_WATER_HAIL.get()), CommonConfigs.WEATHER2_WATER_TORNADO.get()));
            NatureWeather.extraSandstormAt = (level, p) -> level instanceof ServerLevel sl && inside(storms(sl).sand, p);
            NatureWeather.extraSnowstormAt = (level, p) -> level instanceof ServerLevel sl && inside(storms(sl).snow, p);
            ImmersiveNature.LOGGER.info("Weather2 found: its storms count as rain, sandstorms make Immersive Nature sand piles");
        } catch (Throwable t) {
            ImmersiveNature.LOGGER.warn("Weather2 compat disabled, unknown Weather2 version", t);
        }
    }

    private static double[] stormData(ServerLevel level) {
        return storms(level).data;
    }

    private static final Storms NO_STORMS = new Storms();

    private static Storms storms(ServerLevel level) {
        if (!loaded || broken || !CommonConfigs.WEATHER2_RAIN.get()) return NO_STORMS;
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
        if (time - s.time < 20 && time >= s.time) return s;
        double[] data = EMPTY, sand = EMPTY, snow = EMPTY;
        int[] stages = new int[0];
        try {
            Object manager = getManager.invoke(null, level);
            if (manager != null) {
                List<?> storms = (List<?>) getStorms.invoke(manager);
                double[] tmp = new double[storms.size() * 3], tSand = new double[storms.size() * 3], tSnow = new double[storms.size() * 3];
                int[] tStages = new int[storms.size()];
                int n = 0, nSand = 0, nSnow = 0;
                for (Object o : storms) {
                    if (isDead.getBoolean(o)) continue;
                    if (stormClass.isInstance(o)) {
                        if (!(Boolean) isPrecipitating.invoke(o)) continue;
                        Vec3 p = (Vec3) pos.get(o);
                        tStages[n / 3] = stage != null ? stage.getInt(o) : 0;
                        tmp[n++] = p.x;
                        tmp[n++] = p.z;
                        tmp[n++] = size.getInt(o);
                    } else if (particleStormClass != null && particleStormClass.isInstance(o)) {
                        Vec3 p = (Vec3) pos.get(o);
                        int r = (Integer) getSize.invoke(o);
                        Object type = getType.invoke(o);
                        boolean isSand = type instanceof Enum<?> e && e.name().equals("SANDSTORM");
                        double[] t = isSand ? tSand : tSnow;
                        int i = isSand ? nSand : nSnow;
                        t[i] = p.x;
                        t[i + 1] = p.z;
                        t[i + 2] = r;
                        if (isSand) nSand += 3;
                        else nSnow += 3;
                    }
                }
                data = java.util.Arrays.copyOf(tmp, n);
                stages = java.util.Arrays.copyOf(tStages, n / 3);
                sand = java.util.Arrays.copyOf(tSand, nSand);
                snow = java.util.Arrays.copyOf(tSnow, nSnow);
            }
        } catch (Throwable t) {
            broken = true;
            ImmersiveNature.LOGGER.warn("Weather2 compat stopped working", t);
        }
        s.data = data;
        s.stages = stages;
        s.sand = sand;
        s.snow = snow;
        s.time = time;
        return s;
    }

    private static boolean inside(double[] d, BlockPos p) {
        for (int i = 0; i < d.length; i += 3) {
            double dx = p.getX() + 0.5 - d[i], dz = p.getZ() + 0.5 - d[i + 1];
            if (dx * dx + dz * dz < d[i + 2] * d[i + 2]) return true;
        }
        return false;
    }

    /** True if a raining Weather2 storm is over this position (the usual sky and biome checks are done by the caller). */
    public static boolean stormRainsAt(ServerLevel level, BlockPos p) {
        if (!loaded) return false;
        return inside(stormData(level), p);
    }

    /** Rain water amount of the strongest-raining storm over this position (config weather2.rain_water), or -1 if none. */
    public static double rainWaterAt(ServerLevel level, BlockPos p) {
        if (!loaded) return -1;
        Storms s = storms(level);
        double[] d = s.data;
        double best = -1;
        for (int i = 0; i < d.length; i += 3) {
            double dx = p.getX() + 0.5 - d[i], dz = p.getZ() + 0.5 - d[i + 1];
            if (dx * dx + dz * dz >= d[i + 2] * d[i + 2]) continue;
            int st = i / 3 < s.stages.length ? s.stages[i / 3] : 0;
            double m = switch (st) {
                case 0 -> CommonConfigs.WEATHER2_WATER_RAIN.get();
                case 1 -> CommonConfigs.WEATHER2_WATER_LIGHTNING.get();
                case 2 -> CommonConfigs.WEATHER2_WATER_HIGHWIND.get();
                case 3 -> CommonConfigs.WEATHER2_WATER_HAIL.get();
                default -> CommonConfigs.WEATHER2_WATER_TORNADO.get();
            };
            best = Math.max(best, m);
        }
        return best;
    }

    /** Weather2's sand layer block -> Immersive Nature's sand pile (when enabled). */
    public static Block mapSandLayer(Block block) {
        if (block != null && CommonConfigs.WEATHER2_SAND.get() && W2_SAND_LAYER.equals(BuiltInRegistries.BLOCK.getKey(block))) {
            return ModBlocks.SAND_LAYER_BLOCK.get();
        }
        return block;
    }
}
