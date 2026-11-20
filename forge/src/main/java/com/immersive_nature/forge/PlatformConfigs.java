package com.immersive_nature.forge;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.immersive_nature.ImmersiveNature;
import com.immersive_nature.configs.CommonConfigs;
import com.immersive_nature.fluids.FlowingFluids;
import com.immersive_nature.fluids.config.FFConfig;
import com.immersive_nature.forge.overgrowth.Overgrowth;
import com.immersive_nature.forge.overgrowth.configuration.ServerConfiguration;
import com.immersive_nature.snow.SnowCommonConfig;
import com.immersive_nature.snow.client.SnowClientConfig;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.mehvahdjukaar.moonlight.api.platform.configs.ConfigBuilder;
import net.mehvahdjukaar.moonlight.api.platform.configs.forge.ConfigSpecWrapper;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * The sections of the one Immersive Nature config that belong to the Forge-only parts:
 * overgrowth, snow (based on Snow! Real Magic) and fluids (based on Flowing Fluids).
 * Their code keeps reading its own fields; the values are copied there whenever the config loads.
 */
public final class PlatformConfigs {

    private static final List<Runnable> SNOW = new ArrayList<>();
    private static final List<Runnable> SNOW_CLIENT = new ArrayList<>();
    private static final List<FluidEntry<?>> FLUIDS = new ArrayList<>();

    private PlatformConfigs() {
    }

    // ---------------------------------------------------------------- common

    public static void addCommon(ConfigBuilder b) {
        b.comment("Overgrowth: slow changes of the world around you. Each number is how often that change is tried; 0 turns it off")
                .push("overgrowth");
        Overgrowth.setConfig(new ServerConfiguration(b));
        b.pop();

        b.comment("Snow: snow layers that cover plants, slabs, stairs, fences and leaves, fall like sand and pile up during snowfall")
                .push("snow");
        bool(b, SNOW, "gravity", "Snow falls down like sand", true, v -> SnowCommonConfig.snowGravity = v);
        bool(b, SNOW, "making_ice", "Falling snow turns into ice when it lands on water", true, v -> SnowCommonConfig.snowMakingIce = v);
        bool(b, SNOW, "always_replaceable", "Snow can be replaced by any block, no matter how many layers it has", true, v -> SnowCommonConfig.snowAlwaysReplaceable = v);
        bool(b, SNOW, "thinner_bounding_box", "The collision box of snow is halved", true, v -> SnowCommonConfig.thinnerBoundingBox = v);
        bool(b, SNOW, "never_melt", "Snow never melts", false, v -> SnowCommonConfig.snowNeverMelt = v);
        integer(b, SNOW, "spawn_max_light_level", "Snow falling from the sky only stays where the block light is at most this", 9, 0, 15, v -> SnowCommonConfig.snowSpawnMaxLightLevel = v);
        integer(b, SNOW, "persist_max_light_level", "Snow melts where the block light is above this", 11, 0, 15, v -> SnowCommonConfig.snowPersistMaxLightLevel = v);
        bool(b, SNOW, "reduce_fall_damage", "Snow reduces fall damage", true, v -> SnowCommonConfig.snowReduceFallDamage = v);
        bool(b, SNOW, "sustain_grass", "Grass blocks don't turn into dirt under more than one snow layer", true, v -> SnowCommonConfig.sustainGrassIfLayerMoreThanOne = v);
        bool(b, SNOW, "sneak_snowball", "Crouch and right-click snow to take a snowball", true, v -> SnowCommonConfig.sneakSnowball = v);
        bool(b, SNOW, "fancy_snow_on_upper_slab", "Snow placed on a top slab becomes the detailed snow block", true, v -> SnowCommonConfig.fancySnowOnUpperSlab = v);
        integer(b, SNOW, "mob_spawning_max_layers", "Mobs can spawn on snow with at most this many layers", 8, 1, 8, v -> SnowCommonConfig.mobSpawningMaxLayers = v);
        b.push("snow_cover");
        bool(b, SNOW, "place_on_blocks", "Snow can be placed on some blocks (plants, fences, stairs...) to make them snow-covered", true, v -> SnowCommonConfig.placeSnowOnBlock = v);
        bool(b, SNOW, "place_naturally", "Snowfall and world generation also cover such blocks, not only players", true, v -> SnowCommonConfig.placeSnowOnBlockNaturally = v);
        bool(b, SNOW, "replace_worldgen_feature", "Snow made by world generation covers blocks the same way", true, v -> SnowCommonConfig.replaceWorldFeature = v);
        bool(b, SNOW, "retain_original_blocks", "Turn snow-covered blocks back to normal over time (use before removing the mod)", false, v -> SnowCommonConfig.retainOriginalBlocks = v);
        b.pop();
        b.push("accumulation");
        bool(b, SNOW, "during_snowfall", "Snow piles up during normal snowfall", false, v -> SnowCommonConfig.snowAccumulationDuringSnowfall = v);
        bool(b, SNOW, "during_snowstorm", "Snow piles up during thunderstorms", true, v -> SnowCommonConfig.snowAccumulationDuringSnowstorm = v);
        integer(b, SNOW, "max_layers", "The most layers snow piles up to. 9 = no limit", 8, 0, 9, v -> SnowCommonConfig.snowAccumulationMaxLayers = v);
        bool(b, SNOW, "melt_in_warm_biomes", "Snow and ice melt in warm biomes", false, v -> SnowCommonConfig.snowAndIceMeltInWarmBiomes = v);
        bool(b, SNOW, "natural_melting", "Snow thicker than one layer slowly melts down", true, v -> SnowCommonConfig.snowNaturalMelt = v);
        bool(b, SNOW, "winter_only", "With a seasons mod, snow piles up only in winter", false, v -> SnowCommonConfig.accumulationWinterOnly = v);
        integer(b, SNOW, "weather_tick_slowness", "How slowly rain and snow tick. 16 = default, higher = slower", 16, 1, 1000, v -> SnowCommonConfig.weatherTickSlowness = v);
        b.pop();
        b.push("debug");
        bool(b, SNOW, "mob_spawning_command", "Adds the /debugmobspawning command", false, v -> SnowCommonConfig.debugSpawningCommand = v);
        bool(b, SNOW, "prevent_worldgen_log_spam", "Hides harmless log messages from snow-covered blocks during world generation", true, v -> SnowCommonConfig.preventWorldgenLogSpam = v);
        bool(b, SNOW, "skip_mushrooms_in_worldgen", "Don't snow-cover mushrooms during world generation", true, v -> SnowCommonConfig.skipSnowLoggingMushroom = v);
        b.pop();
        b.pop();

        b.comment("Fluids: realistic, mostly finite water and lava that flow level by level. Most of these can also be changed in game with /flowing_fluids")
                .push("fluids");
        FFConfig d = new FFConfig();
        b.push("general");
        fBool(b, "enabled", "Turns the finite fluids on or off", d.enableMod, c -> c.enableMod, (c, v) -> c.enableMod = v);
        fBool(b, "flow_to_edges", "Fluids flow towards nearby edges and holes", d.flowToEdges, c -> c.flowToEdges, (c, v) -> c.flowToEdges = v);
        fInt(b, "water_flow_distance", "How far water looks for an edge to flow to", d.waterFlowDistance, 1, 8, c -> c.waterFlowDistance, (c, v) -> c.waterFlowDistance = v);
        fInt(b, "lava_flow_distance", "How far lava looks for an edge to flow to", d.lavaFlowDistance, 1, 8, c -> c.lavaFlowDistance, (c, v) -> c.lavaFlowDistance = v);
        fInt(b, "lava_nether_flow_distance", "How far lava looks for an edge in the Nether", d.lavaNetherFlowDistance, 1, 8, c -> c.lavaNetherFlowDistance, (c, v) -> c.lavaNetherFlowDistance = v);
        fInt(b, "water_tick_delay", "Ticks between water flow steps", d.waterTickDelay, 1, 255, c -> c.waterTickDelay, (c, v) -> c.waterTickDelay = v);
        fInt(b, "lava_tick_delay", "Ticks between lava flow steps", d.lavaTickDelay, 1, 255, c -> c.lavaTickDelay, (c, v) -> c.lavaTickDelay = v);
        fInt(b, "lava_nether_tick_delay", "Ticks between lava flow steps in the Nether", d.lavaNetherTickDelay, 1, 255, c -> c.lavaNetherTickDelay, (c, v) -> c.lavaNetherTickDelay = v);
        fInt(b, "tick_delay_spread", "Random extra ticks added to the flow delay, so fluids don't all move at once", d.tickDelaySpread, 0, 255, c -> c.tickDelaySpread, (c, v) -> c.tickDelaySpread = v);
        fInt(b, "random_tick_leveling_distance", "How far random ticks look to level out uneven water surfaces", d.randomTickLevelingDistance, 0, 64, c -> c.randomTickLevelingDistance, (c, v) -> c.randomTickLevelingDistance = v);
        fInt(b, "player_block_distance_for_flowing", "Fluids only flow within this many blocks of a player (0 = everywhere)", d.playerBlockDistanceForFlowing, 0, 512, c -> c.playerBlockDistanceForFlowing, (c, v) -> c.playerBlockDistanceForFlowing = v);
        fEnum(b, "water_log_flow_mode", "How water flows in and out of waterlogged blocks", d.waterLogFlowMode, c -> c.waterLogFlowMode, (c, v) -> c.waterLogFlowMode = v);
        fList(b, "fluid_blacklist", "Fluids that keep their normal behaviour, e.g. [\"minecraft:lava\"]", c -> new ArrayList<>(c.fluidBlacklist), (c, v) -> c.fluidBlacklist = new ObjectOpenHashSet<>(v));
        b.pop();
        b.push("refill_and_evaporation");
        fFloat(b, "rain_refill_chance", "Chance for rain to refill water open to the sky", d.rainRefillChance, c -> c.rainRefillChance, (c, v) -> c.rainRefillChance = v);
        fBool(b, "rain_fills_water_higher", "Rain can also raise water that is already one level from full", d.rainFillsWaterHigherV2, c -> c.rainFillsWaterHigherV2, (c, v) -> c.rainFillsWaterHigherV2 = v);
        fFloat(b, "ocean_river_swamp_refill_chance", "Chance for water in oceans, rivers and swamps to refill itself", d.oceanRiverSwampRefillChance, c -> c.oceanRiverSwampRefillChance, (c, v) -> c.oceanRiverSwampRefillChance = v);
        fBool(b, "fast_biome_refill_at_sea_level_only", "Oceans, rivers and swamps only refill at sea level", d.fastBiomeRefillAtSeaLevelOnly, c -> c.fastBiomeRefillAtSeaLevelOnly, (c, v) -> c.fastBiomeRefillAtSeaLevelOnly = v);
        fFloat(b, "infinite_biome_non_consume_chance", "Chance that taking water in an ocean, river or swamp doesn't use it up", d.infiniteWaterBiomeNonConsumeChance, c -> c.infiniteWaterBiomeNonConsumeChance, (c, v) -> c.infiniteWaterBiomeNonConsumeChance = v);
        fFloat(b, "infinite_biome_drain_surface_chance", "Chance that taking water in those biomes lowers the surface instead", d.infiniteWaterBiomeDrainSurfaceChance, c -> c.infiniteWaterBiomeDrainSurfaceChance, (c, v) -> c.infiniteWaterBiomeDrainSurfaceChance = v);
        fFloat(b, "evaporation_chance", "Chance for a single water level to evaporate", d.evaporationChanceV2, c -> c.evaporationChanceV2, (c, v) -> c.evaporationChanceV2 = v);
        fFloat(b, "evaporation_nether_chance", "Chance for water to evaporate in the Nether", d.evaporationNetherChance, c -> c.evaporationNetherChance, (c, v) -> c.evaporationNetherChance = v);
        fFloat(b, "farmland_drain_water_chance", "Chance for farmland to drink a nearby water level", d.farmlandDrainWaterChance, c -> c.farmlandDrainWaterChance, (c, v) -> c.farmlandDrainWaterChance = v);
        fFloat(b, "concrete_drains_water_chance", "Chance that concrete powder uses up the water that hardens it", d.concreteDrainsWaterChance, c -> c.concreteDrainsWaterChance, (c, v) -> c.concreteDrainsWaterChance = v);
        fFloat(b, "animal_breeding_drinks_water_chance", "Chance that breeding animals drink a nearby water level", d.drinkWaterToBreedAnimalChance, c -> c.drinkWaterToBreedAnimalChance, (c, v) -> c.drinkWaterToBreedAnimalChance = v);
        fInt(b, "min_water_level_for_ice", "Water needs at least this many levels to freeze", d.minWaterLevelForIce, 1, 8, c -> c.minWaterLevelForIce, (c, v) -> c.minWaterLevelForIce = v);
        fInt(b, "min_lava_level_for_obsidian", "Lava needs at least this many levels to make obsidian", d.minLavaLevelForObsidian, 1, 8, c -> c.minLavaLevelForObsidian, (c, v) -> c.minLavaLevelForObsidian = v);
        fInt(b, "default_sea_level_override", "Sea level used for refilling (-2147483648 = the world's own)", d.defaultSeaLevelOverride, Integer.MIN_VALUE, Integer.MAX_VALUE, c -> c.defaultSeaLevelOverride, (c, v) -> c.defaultSeaLevelOverride = v);
        fList(b, "dimension_sea_level_overrides", "Sea level per dimension, as \"dimension_id=level\" (ids as used by /flowing_fluids)",
                c -> {
                    List<String> l = new ArrayList<>();
                    c.dimensionSeaLevelOverrides.int2IntEntrySet().forEach(e -> l.add(e.getIntKey() + "=" + e.getIntValue()));
                    return l;
                },
                (c, v) -> {
                    Int2IntOpenHashMap m = new Int2IntOpenHashMap();
                    for (String s : v) {
                        String[] p = s.split("=");
                        try {
                            m.put(Integer.parseInt(p[0].trim()), Integer.parseInt(p[1].trim()));
                        } catch (Exception ignored) {
                        }
                    }
                    c.dimensionSeaLevelOverrides = m;
                });
        b.pop();
        b.push("interactions");
        fBool(b, "enable_displacement", "Blocks placed into fluids push the fluid aside", d.enableDisplacement, c -> c.enableDisplacement, (c, v) -> c.enableDisplacement = v);
        fFloat(b, "displacement_depth_multiplier", "How deep displaced fluid can be pushed", d.displacementDepthMultiplier, c -> c.displacementDepthMultiplier, (c, v) -> c.displacementDepthMultiplier = v);
        fEnum(b, "displacement_sounds", "When displacing fluid plays a sound", d.displacementSounds, c -> c.displacementSounds, (c, v) -> c.displacementSounds = v);
        fBool(b, "enable_piston_pushing", "Pistons push fluids", d.enablePistonPushing, c -> c.enablePistonPushing, (c, v) -> c.enablePistonPushing = v);
        fBool(b, "easy_piston_pump", "A simple piston setup can pump water", d.easyPistonPump, c -> c.easyPistonPump, (c, v) -> c.easyPistonPump = v);
        fBool(b, "flow_affects_boats", "Flowing water pushes boats", d.waterFlowAffectsBoats, c -> c.waterFlowAffectsBoats, (c, v) -> c.waterFlowAffectsBoats = v);
        fBool(b, "flow_affects_entities", "Flowing water pushes mobs", d.waterFlowAffectsEntities, c -> c.waterFlowAffectsEntities, (c, v) -> c.waterFlowAffectsEntities = v);
        fBool(b, "flow_affects_players", "Flowing water pushes players", d.waterFlowAffectsPlayers, c -> c.waterFlowAffectsPlayers, (c, v) -> c.waterFlowAffectsPlayers = v);
        fBool(b, "flow_affects_items", "Flowing water pushes items", d.waterFlowAffectsItems, c -> c.waterFlowAffectsItems, (c, v) -> c.waterFlowAffectsItems = v);
        fBool(b, "enclose_fluids_on_worldgen", "World generation closes off fluids so new chunks don't flood", d.encloseAllFluidOnWorldGen, c -> c.encloseAllFluidOnWorldGen, (c, v) -> c.encloseAllFluidOnWorldGen = v);
        fEnum(b, "create_water_wheel_mode", "Create mod: what a water wheel needs to turn", d.create_waterWheelMode, c -> c.create_waterWheelMode, (c, v) -> c.create_waterWheelMode = v);
        fBool(b, "create_infinite_pipes", "Create mod: open pipes are an infinite water source", d.create_infinitePipes, c -> c.create_infinitePipes, (c, v) -> c.create_infinitePipes = v);
        fInt(b, "create_water_wheel_flow_max_tick_interval", "Create mod: ticks a water wheel keeps turning after the flow stops", d.create_waterWheelFlowMaxTickInterval, 1, 100000, c -> c.create_waterWheelFlowMaxTickInterval, (c, v) -> c.create_waterWheelFlowMaxTickInterval = v);
        b.pop();
        b.push("visuals_and_sound");
        fBool(b, "hide_flowing_texture", "Settled water uses the still water texture", d.hideFlowingTexture, c -> c.hideFlowingTexture, (c, v) -> c.hideFlowingTexture = v);
        fEnum(b, "full_liquid_height", "How high a full fluid block is drawn", d.fullLiquidHeight, c -> c.fullLiquidHeight, (c, v) -> c.fullLiquidHeight = v);
        fFloat(b, "flow_sound_chance", "Chance for flowing water to make a sound", d.flowSoundChance, c -> c.flowSoundChance, (c, v) -> c.flowSoundChance = v);
        b.pop();
        b.push("performance");
        fEnum(b, "auto_performance_mode", "Lowers fluid quality automatically when the server is slow", d.autoPerformanceMode, c -> c.autoPerformanceMode, (c, v) -> c.autoPerformanceMode = v);
        fInt(b, "auto_performance_update_rate_seconds", "Seconds between automatic performance checks", d.autoPerformanceUpdateRateSeconds, 1, 3600, c -> c.autoPerformanceUpdateRateSeconds, (c, v) -> c.autoPerformanceUpdateRateSeconds = v);
        fFloat(b, "auto_performance_mspt_target_multiplier", "Target tick time, as a part of the 50 ms a tick may take", d.autoPerformanceMSPTargetMultiplier, c -> c.autoPerformanceMSPTargetMultiplier, (c, v) -> c.autoPerformanceMSPTargetMultiplier = v);
        fBool(b, "auto_performance_show_messages", "Tell operators when automatic performance changes something", d.autoPerformanceShowMessages, c -> c.autoPerformanceShowMessages, (c, v) -> c.autoPerformanceShowMessages = v);
        b.pop();
        b.push("debug");
        fBool(b, "print_random_ticks", "Log fluid random ticks", d.printRandomTicks, c -> c.printRandomTicks, (c, v) -> c.printRandomTicks = v);
        fBool(b, "debug_water_level_colours", "Colour water by its level", d.debugWaterLevelColours, c -> c.debugWaterLevelColours, (c, v) -> c.debugWaterLevelColours = v);
        fBool(b, "announce_worldgen_actions", "Log what world generation does to fluids", d.announceWorldGenActions, c -> c.announceWorldGenActions, (c, v) -> c.announceWorldGenActions = v);
        fBool(b, "hide_start_message", "Hide the fluids message shown when joining a world", d.hideStartMessage, c -> c.hideStartMessage, (c, v) -> c.hideStartMessage = v);
        b.pop();
        b.pop();
    }

    /** Copies the loaded config into the fields the snow and fluids code reads. */
    public static void applyCommon() {
        SNOW.forEach(Runnable::run);
        loadFluids();
    }

    /** Fresh fluids settings from the config (also used to undo the settings a server sent). */
    public static void loadFluids() {
        FFConfig c = new FFConfig();
        for (FluidEntry<?> e : FLUIDS) e.load(c);
        FlowingFluids.config = c;
    }

    /**
     * The /flowing_fluids commands change the settings in game; this writes them into the config file.
     * The file is written directly: Forge then reads it back and the values update everywhere.
     */
    public static synchronized void saveFluids() {
        FFConfig c = FlowingFluids.config;
        Map<List<String>, Object> changed = new HashMap<>();
        for (FluidEntry<?> e : FLUIDS) e.collect(c, changed);
        try {
            writeInOrder((ConfigSpecWrapper) CommonConfigs.SERVER_SPEC, changed);
        } catch (Exception e) {
            ImmersiveNature.LOGGER.error("Failed to save the fluids settings", e);
        }
    }

    /** Writes the config file with its sections and comments in the usual order. */
    private static void writeInOrder(ConfigSpecWrapper wrapper, Map<List<String>, Object> changed) {
        ForgeConfigSpec spec = wrapper.getSpec();
        try (CommentedFileConfig out = CommentedFileConfig.builder(wrapper.getFullPath()).preserveInsertionOrder().sync().build()) {
            spec.correct(out); // every entry with its comment, in order
            copyValues(spec.getValues(), out);
            changed.forEach(out::set);
            out.save();
        }
    }

    private static void copyValues(UnmodifiableConfig values, CommentedFileConfig out) {
        for (Object o : values.valueMap().values()) {
            if (o instanceof ForgeConfigSpec.ConfigValue<?> v) {
                out.set(v.getPath(), fileValue(v.get()));
            } else if (o instanceof UnmodifiableConfig sub) {
                copyValues(sub, out);
            }
        }
    }

    private static Object fileValue(Object value) {
        return value instanceof Enum<?> e ? e.name() : value;
    }

    // ---------------------------------------------------------------- client

    public static void addClient(ConfigBuilder b) {
        b.push("snow");
        bool(b, SNOW_CLIENT, "particles_through_leaves", "Snowflakes fall under snow-covered leaves", true, v -> SnowClientConfig.particleThroughLeaves = v);
        bool(b, SNOW_CLIENT, "snow_variants", "Some snow-covered plants look different", true, v -> {
            boolean changed = SnowClientConfig.snowVariants != v;
            SnowClientConfig.snowVariants = v;
            if (changed) SnowClientConfig.reloadChunks();
        });
        b.pop();
    }

    public static void applyClient() {
        SNOW_CLIENT.forEach(Runnable::run);
    }

    // ---------------------------------------------------------------- helpers

    private static void bool(ConfigBuilder b, List<Runnable> to, String key, String comment, boolean def, Consumer<Boolean> set) {
        Supplier<Boolean> v = b.comment(comment).define(key, def);
        to.add(() -> set.accept(v.get()));
    }

    private static void integer(ConfigBuilder b, List<Runnable> to, String key, String comment, int def, int min, int max, Consumer<Integer> set) {
        Supplier<Integer> v = b.comment(comment).define(key, def, min, max);
        to.add(() -> set.accept(v.get()));
    }

    private static void fBool(ConfigBuilder b, String key, String comment, boolean def, Function<FFConfig, Boolean> get, BiConsumer<FFConfig, Boolean> set) {
        FLUIDS.add(new FluidEntry<>(b.comment(comment).define(key, def), get, set));
    }

    private static void fInt(ConfigBuilder b, String key, String comment, int def, int min, int max, Function<FFConfig, Integer> get, BiConsumer<FFConfig, Integer> set) {
        FLUIDS.add(new FluidEntry<>(b.comment(comment).define(key, def, min, max), get, set));
    }

    private static void fFloat(ConfigBuilder b, String key, String comment, float def, Function<FFConfig, Float> get, BiConsumer<FFConfig, Float> set) {
        FLUIDS.add(new FluidEntry<>(b.comment(comment).define(key, (double) def, 0, 1000),
                c -> (double) get.apply(c), (c, v) -> set.accept(c, v.floatValue())));
    }

    private static <E extends Enum<E>> void fEnum(ConfigBuilder b, String key, String comment, E def, Function<FFConfig, E> get, BiConsumer<FFConfig, E> set) {
        FLUIDS.add(new FluidEntry<>(b.comment(comment).define(key, def), get, set));
    }

    private static void fList(ConfigBuilder b, String key, String comment, Function<FFConfig, List<String>> get, BiConsumer<FFConfig, List<String>> set) {
        FLUIDS.add(new FluidEntry<>(b.comment(comment).define(key, new ArrayList<String>()), get, set));
    }

    /** One fluids setting: the config value and the FFConfig field it belongs to. */
    private record FluidEntry<T>(Supplier<T> value, Function<FFConfig, T> get, BiConsumer<FFConfig, T> set) {
        void load(FFConfig c) {
            set.accept(c, value.get());
        }

        void collect(FFConfig c, Map<List<String>, Object> out) {
            if (value instanceof ForgeConfigSpec.ConfigValue<?> v) {
                T now = get.apply(c);
                if (!Objects.equals(v.get(), now)) out.put(v.getPath(), fileValue(now));
            }
        }
    }
}
