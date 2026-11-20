package com.immersive_nature.reg;

import com.immersive_nature.ImmersiveNature;
import net.mehvahdjukaar.moonlight.api.misc.RegSupplier;
import net.mehvahdjukaar.moonlight.api.platform.RegHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class ModSoundEvents {
    public static void init() {
    }

    public static Supplier<SoundEvent> ICICLE_CRACK = registerSoundEvent("icicle_crack");

    @NotNull
    private static RegSupplier<SoundEvent> registerSoundEvent(@NotNull String name) {
        ResourceLocation id = ImmersiveNature.res(name);
        return RegHelper.registerSound(id);
    }
}
