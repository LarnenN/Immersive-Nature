package com.ordana.immersive_weathering.forge;

import com.ordana.immersive_weathering.ImmersiveWeathering;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.MissingMappingsEvent;

/**
 * Immersive Nature started as Immersive Weathering. Worlds made with the old mod store
 * "immersive_weathering:*" ids; this maps every one of them to the matching
 * "immersive_nature:*" entry so nothing disappears from existing worlds.
 */
public final class LegacyIdRemapper {
    private static final String OLD = "immersive_weathering";

    private LegacyIdRemapper() {
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void onMissingMappings(MissingMappingsEvent event) {
        for (var mapping : event.getMappings((ResourceKey) event.getKey(), OLD)) {
            remap((MissingMappingsEvent.Mapping) mapping, event.getRegistry());
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void remap(MissingMappingsEvent.Mapping mapping, IForgeRegistry registry) {
        ResourceLocation key = mapping.getKey();
        if (!OLD.equals(key.getNamespace())) return;
        ResourceLocation newKey = new ResourceLocation(ImmersiveWeathering.MOD_ID, key.getPath());
        if (registry.containsKey(newKey)) {
            Object value = registry.getValue(newKey);
            mapping.remap(value);
        }
    }
}
