package com.immersive_nature.integrations;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.world.level.Level;

public class SereneSeasonsCompat {


    @ExpectPlatform
    public static float getNewTemp(float old, Level level) {
        return old;
    }

}
