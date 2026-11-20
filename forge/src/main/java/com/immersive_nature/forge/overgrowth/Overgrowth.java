/*
 * Ported from Overgrowth by ldtteam (https://github.com/ldtteam/Overgrowth), GPL-3.0.
 * Modified for Immersive Nature: handlers that duplicate Immersive Weathering mechanics
 * (grass plants, mossy blocks, mushrooms) were removed, torch burn-out is off by default.
 */
package com.immersive_nature.forge.overgrowth;

import com.immersive_nature.forge.overgrowth.configuration.ServerConfiguration;
import com.immersive_nature.forge.overgrowth.handlers.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class Overgrowth {
    public static final Logger LOGGER = LogManager.getLogger("Immersive Nature/Overgrowth");

    // the "overgrowth" section of the Immersive Nature config, set up by PlatformConfigs
    public static ServerConfiguration config;

    private Overgrowth() {
    }

    public static void setConfig(ServerConfiguration serverConfig) {
        config = serverConfig;
    }

    public static void init() {
        ITransformationHandler.HANDLERS.add(new GrowWaterPlants());
        ITransformationHandler.HANDLERS.add(new SpiderWebs());
        ITransformationHandler.HANDLERS.add(new ByeTorch());
        ITransformationHandler.HANDLERS.add(new DegradeFarmland());
        ITransformationHandler.HANDLERS.add(new CrackedBlocksDegradation());
        ITransformationHandler.HANDLERS.add(new CoastDecline());
        ITransformationHandler.HANDLERS.add(new LavaDry());
        ITransformationHandler.HANDLERS.add(new NightCampfire());
        ITransformationHandler.HANDLERS.add(new SandToSandstone());
        ITransformationHandler.HANDLERS.add(new UndoPath());
        ITransformationHandler.HANDLERS.add(new MuddyRain());
        ITransformationHandler.HANDLERS.add(new MudDry());
        ITransformationHandler.HANDLERS.add(new NyliumSpread());
        ITransformationHandler.HANDLERS.add(new SpawnEntities());


        // Entity paths are handled by Immersive Nature's Desire Paths (grass, podzol)
    }
}
