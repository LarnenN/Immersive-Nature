package com.immersive_nature.configs;

import com.immersive_nature.ImmersiveWeathering;
import net.mehvahdjukaar.moonlight.api.platform.PlatHelper;
import net.mehvahdjukaar.moonlight.api.platform.configs.ConfigBuilder;
import net.mehvahdjukaar.moonlight.api.platform.configs.ConfigSpec;
import net.mehvahdjukaar.moonlight.api.platform.configs.ConfigType;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class CommonConfigs {


    public static final ConfigSpec SERVER_SPEC;

    public static final Supplier<Boolean> BLOCK_GROWTHS;
    public static final Supplier<Boolean> WATER_EROSION;
    public static final Supplier<Boolean> WATER_EROSION_FARMLAND;
    public static final Supplier<Boolean> WATER_EROSION_MOSS;
    public static final Supplier<Boolean> WATER_EROSION_SOURCE_BREAK;
    public static final Supplier<Boolean> RAIN_WATER;
    public static final Supplier<Integer> RAIN_WATER_CHANCE;
    public static final Supplier<Integer> RAIN_WATER_RADIUS;
    public static final Supplier<Integer> RAIN_WATER_AMOUNT;
    public static final Supplier<Boolean> RAIN_WATER_OTHER_DIMENSIONS;
    public static final Supplier<Boolean> RAIN_WATER_CAN_DISABLE;
    public static final Supplier<Boolean> WATER_SOAKING;
    public static final Supplier<Boolean> WATER_SOAKING_CONSUME;
    public static final Supplier<Boolean> WATER_SOAKING_GRASS_DIES_COARSE;
    public static final Supplier<List<String>> WATER_SOAKING_TRANSFORMATIONS;
    public static final Supplier<Boolean> DESIRE_PATHS;
    public static final Supplier<Integer> DESIRE_PATHS_WEAR;
    public static final Supplier<Boolean> DESIRE_PATHS_MOBS;
    public static final Supplier<Integer> DESIRE_PATHS_FORGET;
    public static final Supplier<Boolean> DESIRE_PATHS_SPEED_BASED;
    public static final Supplier<Double> DESIRE_PATHS_FEATHER_FALLING;
    public static final Supplier<Boolean> DESIRE_PATHS_CROUCH_PREVENTS;
    public static final Supplier<Boolean> DESIRE_PATHS_BREAK_TALL_GRASS;
    public static final Supplier<List<String>> DESIRE_PATHS_TRANSFORMATIONS;
    public static final Supplier<List<String>> DISABLED_GROWTHS;
    public static final Supplier<Double> PLANT_GROWTH_MULTIPLIER;
    public static final Supplier<Integer> WET_CLAY_WATER_LEVELS;
    public static final Supplier<Boolean> CREATIVE_TAB;
    public static final Supplier<Boolean> CREATIVE_DROP;
    public static final Supplier<Boolean> DEBUG_RESOURCES;

    public static final Supplier<Double> MOSS_INTERESTS_FOR_FACE;
    public static final Supplier<Double> MOSS_PATCHINESS;
    public static final Supplier<Double> MOSS_IMMUNE_CHANCE;
    public static final Supplier<Boolean> MOSS_NEEDS_AIR;
    public static final Supplier<Boolean> MOSS_SPREADING_ENABLED;


    public static final Supplier<Boolean> CRACKING_DROPS_BRICK;
    public static final Supplier<Double> CRACK_INTERESTS_FOR_FACE;
    public static final Supplier<Double> CRACK_PATCHINESS;
    public static final Supplier<Double> CRACK_IMMUNE_CHANCE;
    public static final Supplier<Boolean> CRACK_NEEDS_AIR;
    public static final Supplier<Boolean> CRACK_SPREADING_ENABLED;


    public static final Supplier<Boolean> FALLING_ICICLES;
    public static final Supplier<Integer> ICICLE_RARITY;
    public static final Supplier<Boolean> DISABLE_ICICLES;

    public static final Supplier<Integer> FREEZING_ICICLE_SEVERITY;

    public static final Supplier<Double> FIRE_CHARS_WOOD_CHANCE;
    public static final Supplier<Boolean> SOOT_SPAWN;
    public static final Supplier<Double> ASH_SPAWN_CHANCE;
    public static final Supplier<Boolean> FLAMMABLE_COBWEBS;


    public static final Supplier<Boolean> PICKAXE_CRACKING;
    public static final Supplier<Boolean> PICKAXE_CRACKING_SHIFT;
    public static final Supplier<Boolean> BRICK_REPAIRING;
    public static final Supplier<Boolean> AZALEA_SHEARING;
    public static final Supplier<Boolean> MOSS_SHEARING;
    public static final Supplier<Boolean> MOSS_BURNING;
    public static final Supplier<Boolean> SHOVEL_EXTINGUISH;
    public static final Supplier<Boolean> SPONGE_RUSTING;
    public static final Supplier<Boolean> SPONGE_RUST_DRYING;
    public static final Supplier<Boolean> AXE_STRIPPING;
    public static final Supplier<Boolean> AXE_SCRAPING;


    public static final Supplier<Boolean> ICICLE_FOOD;
    public static final Supplier<Boolean> ICICLE_FIRE_RESISTANCE;

    public static final Supplier<Boolean> COMPOSTER_DIRT;
    public static final Supplier<Boolean> LEGGINGS_PREVENTS_THORN_DAMAGE;
    public static final Supplier<String> GENERIC_BARK;
    public static final Supplier<Boolean> GRASS_OVER_MYCELIUM;
    public static final Supplier<Boolean> MYCELIUM_OVER_GRASS;

    public static final Supplier<Boolean> LEAF_PILES_SLOW;
    public static final Supplier<Double> LEAF_PILES_FROM_DECAY_CHANCE;
    public static final Supplier<Integer> LEAF_PILE_MAX_HEIGHT;
    public static final Supplier<Integer> LEAF_PILES_REACH;
    public static final Supplier<List<String>> LEAF_PILES_BLACKLIST;


    public static final Supplier<Boolean> THIN_ICE_MELTING;

    public static final Supplier<Boolean> VITRIFIED_LIGHTNING;
    public static final Supplier<Double> FULGURITE_CHANCE;


    public static final Supplier<Boolean> RUSTING;
    public static final Supplier<Integer> RUSTING_INFLUENCE_RADIUS;
    public static final Supplier<Double> RUSTING_RATE;
    public static final Supplier<Boolean> RUST_STREAKING;


    public static void init() {
    }

    static{
        ConfigBuilder builder = ConfigBuilder.create(ImmersiveWeathering.res("common"), ConfigType.COMMON);

        builder.setSynced();

        builder.push("general");
        BLOCK_GROWTHS = builder.define("block_growths", true);
        DISABLED_GROWTHS = builder.comment("put here the name of a block growth json you want to disable i.e: [weeds, weeds_spread]." +
                        "Note that this is not the preferred way to do this as block growths are all data driven so it would be best to disable or tweak them by creating a datapack that overrides them" +
                        "Check the mod data folder for the required names. Requires resource reload (/data reload)")
                .define("block_growth_blacklist", new ArrayList<>());
        PLANT_GROWTH_MULTIPLIER = builder.comment("Speeds up grass, ferns, flowers and other plants growing on grass blocks (1 = original Immersive Weathering speed). Plants grow next to other grass plants")
                .define("plant_growth_multiplier", 1.0, 0, 100);
        WET_CLAY_WATER_LEVELS = builder.comment("How many water levels wet earthen clay leaves behind when broken (0 = none, 8 = a full water block). Uses Flowing Fluids levels")
                .define("wet_earthen_clay_water_levels", 4, 0, 8);
        CREATIVE_TAB = builder.define("creative_tab", false);
        CREATIVE_DROP = builder.comment("Drop stuff when in creative").define("drop_in_creative", false);
        DEBUG_RESOURCES = builder.comment("Save generated resources to disk in a 'debug' folder in your game directory. Mainly for debug purposes but can be used to generate assets in all wood types for your mods :0")
                .define("debug_save_dynamic_pack", false);

        builder.pop();

        builder.push("mossy_blocks");
        MOSS_INTERESTS_FOR_FACE = builder.define("interest_for_face", 0.3, 0, 1d);
        MOSS_PATCHINESS = builder.define("patchiness", 0.5, 0, 1);
        MOSS_IMMUNE_CHANCE = builder.define("immune_chance", 0.4, 0, 1);
        MOSS_NEEDS_AIR = builder.define("needs_air", true);
        MOSS_SPREADING_ENABLED = builder.define("enabled", true);
        builder.pop();

        builder.push("cracked_blocks");
        CRACKING_DROPS_BRICK = builder.define("cracking_drops_brick", false);
        CRACK_INTERESTS_FOR_FACE = builder.define("interest_for_face", 0.6, 0, 1d);
        CRACK_PATCHINESS = builder.define("patchiness", 0.4, 0, 1);
        CRACK_IMMUNE_CHANCE = builder.define("immune_chance", 0.4, 0, 1);
        CRACK_NEEDS_AIR = builder.define("needs_air", false);
        CRACK_SPREADING_ENABLED = builder.define("enabled", true);

        builder.pop();

        builder.push("icicle");
        FALLING_ICICLES = builder.define("react_to_vibrations", true);
        ICICLE_RARITY = builder.define("spawn_rarity", 12, 1, 100);
        DISABLE_ICICLES = builder.define("disable_icicles", false);
        builder.pop();

        builder.push("freezing");
        //all these are disabled when at 0 of course
        FREEZING_ICICLE_SEVERITY = builder.define("icicle", 300, 0, 1000);
        builder.pop();
        builder.setSynced();

        builder.push("charring");
        FIRE_CHARS_WOOD_CHANCE = builder.define("fire_chars_wood", 0.3, 0, 1);
        SOOT_SPAWN = builder.define("soot_spawn", true);
        ASH_SPAWN_CHANCE = builder.comment("Chance for a pile of ash to be left when fire burns a block that has no charred variant")
                .define("ash_spawn", 0.3, 0, 1);
        FLAMMABLE_COBWEBS = builder.define("flammable_cobweb", true);
        builder.pop();


        builder.push("item_interaction");
        PICKAXE_CRACKING = builder.define("pickaxe_cracking", true);
        PICKAXE_CRACKING_SHIFT = builder.define("pickaxe_cracking_shift", false);
        BRICK_REPAIRING = builder.define("brick_breaking", true);
        AZALEA_SHEARING = builder.define("azalea_shearing", true);
        MOSS_SHEARING = builder.define("moss_shearing", true);
        MOSS_BURNING = builder.define("moss_burning", true);
        SHOVEL_EXTINGUISH = builder.define("shovel_extinguish", true);
        SPONGE_RUSTING = builder.define("sponge_rusting", true);
        SPONGE_RUST_DRYING = builder.define("sponge_rust_drying", false);
        AXE_STRIPPING = builder.define("axe_stripping", true);
        AXE_SCRAPING = builder.define("axe_rusting", true);
        builder.pop();

        builder.push("food");
        ICICLE_FOOD = builder.define("icicle_food", true);
        ICICLE_FIRE_RESISTANCE = builder.define("icicle_fire_resistance", true);
        builder.pop();

        builder.push("misc");
        COMPOSTER_DIRT = builder.define("composter_dirt", true);
        GRASS_OVER_MYCELIUM = builder.define("grass_over_mycelium", true);
        MYCELIUM_OVER_GRASS = builder.define("mycelium_over_grass", true);
        GENERIC_BARK = builder.define("generic_bark", "");
        LEGGINGS_PREVENTS_THORN_DAMAGE = builder.define("leggings_prevents_thorn_damage", true);
        builder.pop();

        builder.push("leaf_piles");
        LEAF_PILES_SLOW = builder.define("leaf_piles_slow", true);
        LEAF_PILES_FROM_DECAY_CHANCE = builder.define("spawn_entity_from_decay", 0.3, 0, 1);

        LEAF_PILES_REACH = builder.define("reach", 12, 1, 256);
        LEAF_PILE_MAX_HEIGHT = builder.define("max_pile_height", 3, 1, 8);
        LEAF_PILES_BLACKLIST = builder.comment("leaves that wont spawn leaf piles").define("leaf_piles_blacklist", List.of());
        builder.pop();

        builder.push("thin_ice");
        THIN_ICE_MELTING = builder.define("natural_melting", false);
        builder.pop();

        builder.push("lightning_growths"); //TODO:move to data
        VITRIFIED_LIGHTNING = builder.define("vitrified_lightning", true);
        FULGURITE_CHANCE = builder.comment("chance that a lightning strike on sand creates fulgurite")
                .define("fulgurite_chance", 0.4, 0, 1);
        builder.pop();

        builder.push("rusting");
        RUSTING = builder.define("rusting", true);
        RUSTING_INFLUENCE_RADIUS = builder.define("rusting_influence_radius", 4, 1, 8);
        RUSTING_RATE = builder.define("rusting_rate", 0.06, 0, 1);
        RUST_STREAKING = builder.define("rust_streaking", true);
        builder.pop();

        //fabric specific
        PlatHelper.getPlatform().ifFabric(() -> {


        });


        // ported from Water Erosion by 13rac1 (GPL-3.0)
        builder.push("water_erosion");
        WATER_EROSION = builder.comment("Flowing water slowly erodes dirt, sand, gravel and similar blocks, carving streams and waterfalls")
                .define("enabled", true);
        WATER_EROSION_FARMLAND = builder.comment("Allow water to erode and decay farmland")
                .define("erode_farmland", true);
        WATER_EROSION_MOSS = builder.comment("Cobblestone and stone bricks next to flowing water slowly turn mossy")
                .define("moss_near_water", true);
        WATER_EROSION_SOURCE_BREAK = builder.comment("Still water can break through weak walls next to it")
                .define("source_break", true);
        builder.pop();

        builder.push("rain_water");
        RAIN_WATER = builder.comment("While it rains, water slowly gathers on the ground around players (best with Flowing Fluids)")
                .define("enabled", true);
        RAIN_WATER_CHANCE = builder.comment("Every tick, each player has a 1 in this number chance to make a spot of rain water nearby")
                .define("chance", 200, 1, 100000);
        RAIN_WATER_RADIUS = builder.comment("How far from the player (in blocks) rain water can appear")
                .define("radius", 20, 1, 128);
        RAIN_WATER_AMOUNT = builder.comment("How many water levels each raindrop spot adds (1-8, Flowing Fluids levels): 8 = a full water block, 1 = a thin puddle. The water then flows by Flowing Fluids physics")
                .define("amount", 4, 1, 8);
        RAIN_WATER_OTHER_DIMENSIONS = builder.comment("Also gather rain water in dimensions other than the Overworld")
                .define("other_dimensions", false);
        RAIN_WATER_CAN_DISABLE = builder.comment("Allow players to switch rain water off for themselves with /disable_enable_rain_water")
                .define("can_disable", true);
        builder.pop();

        builder.push("water_soaking");
        WATER_SOAKING = builder.comment("Water standing on soil slowly soaks into it and changes the block below")
                .define("enabled", true);
        WATER_SOAKING_CONSUME = builder.comment("Soaking uses up one water level (only with Flowing Fluids, so puddles slowly dry into the ground)")
                .define("consume_water", true);
        WATER_SOAKING_GRASS_DIES_COARSE = builder.comment("Grass that dies under a full block of water picks its result from the rules below too (by their chances) instead of always dirt")
                .define("drowned_grass_uses_rules", true);
        WATER_SOAKING_TRANSFORMATIONS = builder.comment("What soaks into what, as \"from,to,chance\". Chance is per random tick of the water above (0.05 = 5%). One block can have several results, e.g. grass -> dirt, coarse dirt or mud, each with its own chance. From can be a block id or a #block_tag")
                .define("rules", List.of(
                        "minecraft:grass_block,minecraft:dirt,0.06",
                        "minecraft:grass_block,minecraft:coarse_dirt,0.04",
                        "minecraft:grass_block,minecraft:mud,0.02",
                        "minecraft:dirt,minecraft:mud,0.05",
                        "minecraft:coarse_dirt,minecraft:mud,0.02"));
        builder.pop();

        builder.push("desire_paths");
        DESIRE_PATHS = builder.comment("Walking over grass and podzol slowly wears it down into dirt and then coarse dirt, forming trails where people and mobs walk often")
                .define("enabled", true);
        DESIRE_PATHS_WEAR = builder.comment("Every block remembers how much it was walked on. Each tick someone walks on it adds 1 (sprinting more, sneaking less); at this amount it wears down one step. About 5 ticks = one walk across a block")
                .define("wear_to_change", 100, 1, 100000);
        DESIRE_PATHS_MOBS = builder.comment("Mobs wear down paths too (except the ones in the entity tag immersive_nature:no_desire_paths, sheep by default). If false, only players and mobs in immersive_nature:makes_desire_paths do")
                .define("mobs_make_paths", true);
        DESIRE_PATHS_FORGET = builder.comment("Minutes after which a block forgets that it was walked on, if nobody walks on it again")
                .define("forget_after_minutes", 20, 1, 100000);
        DESIRE_PATHS_SPEED_BASED = builder.comment("Sprinting wears paths faster, sneaking-speed movement slower (walking adds 1 per tick)")
                .define("speed_based", true);
        DESIRE_PATHS_FEATHER_FALLING = builder.comment("Each level of Feather Falling on your boots lowers the wear by this part (0.25 = Feather Falling IV stops it)")
                .define("feather_falling_reduction", 0.25, 0, 1);
        DESIRE_PATHS_CROUCH_PREVENTS = builder.comment("Crouching does not wear down the ground")
                .define("crouch_prevents", true);
        DESIRE_PATHS_BREAK_TALL_GRASS = builder.comment("Grass and ferns on top get trampled when the block below wears down")
                .define("break_tall_grass", true);
        DESIRE_PATHS_TRANSFORMATIONS = builder.comment("What wears into what, as \"from,to\". From can be a block id or a #block_tag")
                .define("transformations", List.of(
                        "minecraft:grass_block,minecraft:dirt",
                        "minecraft:podzol,minecraft:dirt",
                        "minecraft:dirt,minecraft:coarse_dirt"));
        builder.pop();

        SERVER_SPEC = builder.buildAndRegister();
        SERVER_SPEC.loadFromFile();
    }


    //stuff belows represents the configs that need to be added


    //moss


    // todo: LEAF_PILES_BLACKLIST tag


    //leaf piles


    public static LeafPileMode fallenLeafPiles() {
        throw new AssertionError();
    }

    public enum LeafPileMode {
        LEAF_LAYER, SIMPLE, OFF
    }


    //fire stuff


    public static boolean vitrifiedSand() {
        throw new AssertionError();
    }


    public static boolean fulgurite() {
        throw new AssertionError();
    }

    //frost


    public static boolean naturalIceMelt() { //?
        throw new AssertionError();
    }

    public static boolean iciclePlacement() { //?
        throw new AssertionError();
    }

    //food

    public static boolean VITRIFIED_LAVA;

    //here are forge old config values. only here for their old descriptionKey


}
