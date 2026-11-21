package com.immersive_nature.data.block_growths;

import com.google.common.base.Suppliers;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.immersive_nature.ImmersiveNature;
import com.immersive_nature.configs.CommonConfigs;
import com.immersive_nature.data.block_growths.growths.ConfigurableBlockGrowth;
import com.immersive_nature.data.block_growths.growths.IBlockGrowth;
import com.immersive_nature.data.block_growths.growths.builtin.BuiltinBlockGrowth;
import com.immersive_nature.data.block_growths.growths.builtin.LeavesGrowth;
import com.immersive_nature.data.block_growths.growths.builtin.NoOpBlockGrowth;
import net.minecraft.world.level.block.LeavesBlock;
import net.mehvahdjukaar.moonlight.api.misc.RegistryAccessJsonReloadListener;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;

import java.io.FileWriter;
import java.util.*;
import java.util.function.Supplier;

public class BlockGrowthHandler extends RegistryAccessJsonReloadListener {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create(); //json object that will write stuff

    public static final BlockGrowthHandler RELOAD_INSTANCE = new BlockGrowthHandler();

    //these need to be as fast as possible as tick will be called very often
    //block specific growth. fast access with map
    private static final Map<TickSource, Map<Block, Set<IBlockGrowth>>> GROWTH_FOR_BLOCK = new EnumMap<>(TickSource.class);
    //set or universal ones
    private static final Map<TickSource, Set<IBlockGrowth>> UNIVERSAL_GROWTHS = new EnumMap<>(TickSource.class);
    //fallback growth for leaves from other mods (e.g. Dynamic Trees) that have no own entry
    private static IBlockGrowth LEAF_GROWTH = null;

    public BlockGrowthHandler() {
        super(GSON, "block_growths");
    }

    public static Optional<Set<IBlockGrowth>> getBlockGrowths(TickSource source, Block block) {
        return Optional.ofNullable(GROWTH_FOR_BLOCK.get(source)).map(m -> m.get(block));
    }

    public static void tickBlock(TickSource source, BlockState state, ServerLevel level, BlockPos pos) {
        if (!CommonConfigs.BLOCK_GROWTHS.get()) return;

        if (state.getBlock() instanceof IConditionalGrowingBlock cb && !cb.canGrow(state)) return;

        var universalGroup = UNIVERSAL_GROWTHS.get(source);
        // Immersive Nature: most blocks (stone, logs, sand...) have no growths at all, so stop
        // before creating the biome lookup and other per-tick objects
        Map<Block, Set<IBlockGrowth>> forSource = GROWTH_FOR_BLOCK.get(source);
        boolean hasOwn = forSource != null && forSource.containsKey(state.getBlock());
        if (!hasOwn && (universalGroup == null || universalGroup.isEmpty())
                && !(source == TickSource.BLOCK_TICK && LEAF_GROWTH != null && state.getBlock() instanceof LeavesBlock)) {
            return;
        }

        Supplier<Holder<Biome>> biome = Suppliers.memoize(() -> level.getBiome(pos));

        if (universalGroup != null) {
            for (var config : universalGroup) {
                config.tryGrowing(pos, state, level, biome);
            }
        }
        var growth = getBlockGrowths(source, state.getBlock());
        if (growth.isEmpty() && source == TickSource.BLOCK_TICK && LEAF_GROWTH != null && state.getBlock() instanceof LeavesBlock) {
            LEAF_GROWTH.tryGrowing(pos, state, level, biome);
        } else if (growth.isPresent()) {
            for (var config : growth.get()) {
                config.tryGrowing(pos, state, level, biome);
            }
        }
    }

    //called by mixin
    public static void performSkyAccessTick(ServerLevel level, LevelChunk levelChunk, int randomTickSpeed) {
        ChunkPos chunkpos = levelChunk.getPos();

        float chance = randomTickSpeed / (3f * 16f);
        int minX = chunkpos.getMinBlockX();
        int minZ = chunkpos.getMinBlockZ();
        boolean isRaining = com.immersive_nature.util.NatureWeather.isRaining(level);
        do {
            if (chance > level.getRandom().nextFloat()) {

                BlockPos firstAirPos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, level.getBlockRandomPos(minX, 0, minZ, 15));
                BlockPos targetPos = firstAirPos.below();
                BlockState state = level.getBlockState(targetPos);

                TickSource source = TickSource.CLEAR_SKY;
                if (isRaining && com.immersive_nature.util.NatureWeather.precipitatesAt(level, targetPos)) {
                    Biome biome = level.getBiome(targetPos).value();
                    Biome.Precipitation precipitation = biome.getPrecipitationAt(targetPos);
                    source = precipitation == Biome.Precipitation.SNOW ? TickSource.SNOW : TickSource.RAIN;
                }
                tickBlock(source, state, level, new BlockPos(targetPos));
            }
            chance--;
        } while (chance > 0);
    }

    @Override
    public void parse(Map<ResourceLocation, JsonElement> jsonMap, RegistryAccess registryAccess) {
        Map<ResourceLocation, JsonElement> map = new HashMap<>();
        for (var e : jsonMap.entrySet()) {
            map.put(e.getKey(), e.getValue().deepCopy());
        }

        List<IBlockGrowth> growths = new ArrayList<>();

        for (var e : map.entrySet()) {
            String name = e.getKey().getPath();
            //blacklist
            if (CommonConfigs.DISABLED_GROWTHS.get().contains(name.toLowerCase(Locale.ROOT))) continue;

            var json = e.getValue();
            // Immersive Nature: the lone grass sprouting chance comes from the config
            if (name.equals("grass_block_seeding") && json instanceof JsonObject seed && seed.has("growth_chance")) {
                seed.addProperty("growth_chance", CommonConfigs.GRASS_SEEDING_CHANCE.get().floatValue());
            }
            DataResult<? extends IBlockGrowth> result;
            if (json instanceof JsonObject jo && jo.has("builtin")) {
                result = BuiltinBlockGrowth.CODEC.parse(RegistryOps.create(JsonOps.INSTANCE, registryAccess), json);
            } else {
                result = ConfigurableBlockGrowth.CODEC.parse(RegistryOps.create(JsonOps.INSTANCE, registryAccess), json);
            }
            var o = result.resultOrPartial(error -> ImmersiveNature.LOGGER.error("Failed to read block growth JSON object for {} : {}", e.getKey(), error));
            if (o.isPresent()) {
                IBlockGrowth g = o.get();
                if (!(g instanceof NoOpBlockGrowth)) {
                    growths.add(g);
                }
            }
            o.ifPresent(growths::add);
        }
        ImmersiveNature.LOGGER.info("Loaded {} block growths configurations", map.size());

        GROWTH_FOR_BLOCK.clear();
        UNIVERSAL_GROWTHS.clear();
        LEAF_GROWTH = null;
        for (var config : growths) {
            if (config instanceof LeavesGrowth) LEAF_GROWTH = config;
        }

        for (var config : growths) {

            var sources = config.getTickSources();
            for (var s : sources) {
                var owners = config.getOwners();

                if (owners == null) { //null owners mean it applies to everything
                    var group = UNIVERSAL_GROWTHS.computeIfAbsent(s, e -> new HashSet<>());
                    group.add(config);
                } else {
                    var group = GROWTH_FOR_BLOCK.computeIfAbsent(s, e -> Maps.newIdentityHashMap());
                    config.getOwners().forEach(b -> {

                        group.computeIfAbsent(b, k -> new HashSet<>()).add(config);
                    });
                }

            }
        }
    }

    //debug

    private void writeToFile(final ConfigurableBlockGrowth obj, FileWriter writer) {
        DataResult<JsonElement> r = ConfigurableBlockGrowth.CODEC.encodeStart(JsonOps.INSTANCE, obj);
        r.result().ifPresent(a -> GSON.toJson(sortJson(a.getAsJsonObject()), writer));
    }

    private JsonObject sortJson(JsonObject jsonObject) {
        try {
            Map<String, JsonElement> joToMap = new TreeMap<>();
            jsonObject.entrySet().forEach(e -> {
                var j = e.getValue();
                if (j instanceof JsonObject jo) j = sortJson(jo);
                joToMap.put(e.getKey(), j);
            });
            JsonObject sortedJSON = new JsonObject();
            joToMap.forEach(sortedJSON::add);
            return sortedJSON;
        } catch (Exception ignored) {
        }
        return jsonObject;
    }
}
