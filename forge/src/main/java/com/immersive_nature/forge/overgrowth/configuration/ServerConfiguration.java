// Ported from Overgrowth by ldtteam (https://github.com/ldtteam/Overgrowth), GPL-3.0. Modified for Immersive Nature.
package com.immersive_nature.forge.overgrowth.configuration;

import net.mehvahdjukaar.moonlight.api.platform.configs.ConfigBuilder;
import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Overgrowth settings: the "overgrowth" section of the Immersive Nature config.
 */
public class ServerConfiguration
{
    public final ForgeConfigSpec.IntValue globalspeed;
    public final ForgeConfigSpec.IntValue byetorch;
    public final ForgeConfigSpec.IntValue coastdecline;
    public final ForgeConfigSpec.IntValue lavadegration;
    public final ForgeConfigSpec.IntValue farmlanddegration;
    public final ForgeConfigSpec.IntValue underwaterplants;
    public final ForgeConfigSpec.IntValue drylava;
    public final ForgeConfigSpec.IntValue drymud;
    public final ForgeConfigSpec.IntValue spreadmud;
    public final ForgeConfigSpec.IntValue byecampfire;
    public final ForgeConfigSpec.IntValue anvilsand;
    public final ForgeConfigSpec.IntValue spiderwebs;
    public final ForgeConfigSpec.IntValue byepath;
    public final ForgeConfigSpec.IntValue entityspawn;
    public final ForgeConfigSpec.IntValue spreadnylium;
    public final ForgeConfigSpec.BooleanValue caveentities;

    public ServerConfiguration(final ConfigBuilder builder)
    {
        globalspeed = rate(builder, "global_speed", "How often the changes below are tried in general. 0 turns all of them off", 80);
        byetorch = rate(builder, "torches_burn_out", "Torches burn out (off by default, Hardcore Torches does this better)", 0);
        coastdecline = rate(builder, "coast_decline", "Sand, gravel and dirt at the water's edge slowly wash away", 18);
        lavadegration = rate(builder, "lava_degradation", "Stone and bricks slowly crack and crumble into cobblestone", 17);
        farmlanddegration = rate(builder, "farmland_degradation", "Farmland slowly degrades", 16);
        underwaterplants = rate(builder, "underwater_plants", "Seagrass and corals grow under water", 15);
        drylava = rate(builder, "lava_drying", "Small pools of lava cool into cobblestone", 17);
        drymud = rate(builder, "mud_drying", "Mud away from water dries into dirt when it doesn't rain", 7);
        spreadmud = rate(builder, "mud_in_rain", "Rain turns dirt and grass under the open sky into mud", 64);
        byecampfire = rate(builder, "campfires_go_out", "Campfires go out over time unless they stand on a hay bale", 11);
        anvilsand = rate(builder, "anvil_sand_to_sandstone", "An anvil presses the sand under it into sandstone", 13);
        spiderwebs = rate(builder, "spider_webs", "Cobwebs appear around planks and stone bricks", 15);
        byepath = rate(builder, "path_decay", "Dirt paths turn back into dirt", 29);
        entityspawn = rate(builder, "entity_spawning", "Farm animals (cows, sheep, chickens, pigs) appear on grass over time", 23);
        spreadnylium = rate(builder, "nylium_spread", "Nylium spreads over netherrack", 24);
        caveentities = (ForgeConfigSpec.BooleanValue) builder.comment("Neutral animals can spawn in caves").define("cave_animals", false);
    }

    private static ForgeConfigSpec.IntValue rate(ConfigBuilder builder, String key, String comment, int def)
    {
        return (ForgeConfigSpec.IntValue) builder.comment(comment).define(key, def, 0, 1000);
    }
}
