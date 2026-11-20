package com.immersive_nature.forge;

import com.immersive_nature.ImmersiveNature;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.MissingMappingsEvent;
import net.minecraftforge.registries.RegistryManager;

import net.minecraft.nbt.CompoundTag;

import java.util.Map;

/**
 * Keeps old worlds working. Immersive Nature started as Immersive Weathering, and its snow and
 * fluids used to be the separate mods Snow! Real Magic and Flowing Fluids. Worlds made with those
 * store "immersive_weathering:*" and "snowrealmagic:*" ids; this maps each one to the matching
 * "immersive_nature:*" entry so nothing disappears.
 */
public final class LegacyIdRemapper {
    private static final String WEATHERING = "immersive_weathering";
    private static final String SNOW = "snowrealmagic";
    // Snow! Real Magic ids that got a clearer name
    private static final Map<String, String> SNOW_NAMES = Map.of(
            "snow", "snow_layer",
            "fence", "snowy_fence",
            "fence2", "snowy_fence2",
            "stairs", "snowy_stairs",
            "slab", "snowy_slab",
            "fence_gate", "snowy_fence_gate",
            "wall", "snowy_wall",
            "texture_tile", "snow_covered_block",
            "normalize", "snow_normalize");

    private LegacyIdRemapper() {
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void onMissingMappings(MissingMappingsEvent event) {
        // the registry the event hands out does not hold the entries yet, the active one does
        IForgeRegistry registry = RegistryManager.ACTIVE.getRegistry((ResourceKey) event.getKey());
        if (registry == null) return;
        for (String namespace : new String[]{WEATHERING, SNOW}) {
            for (var mapping : event.getMappings((ResourceKey) event.getKey(), namespace)) {
                remap((MissingMappingsEvent.Mapping) mapping, registry);
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void remap(MissingMappingsEvent.Mapping mapping, IForgeRegistry registry) {
        ResourceLocation key = mapping.getKey();
        String path = key.getPath();
        if (SNOW.equals(key.getNamespace())) {
            // the falling snow entity was "snow" too, like the snow layer block
            path = registry.getRegistryKey().location().getPath().equals("entity_type") ? "falling_snow" : SNOW_NAMES.getOrDefault(path, path);
        } else if (!WEATHERING.equals(key.getNamespace())) {
            return;
        }
        ResourceLocation newKey = new ResourceLocation(ImmersiveNature.MOD_ID, path);
        if (registry.containsKey(newKey)) {
            mapping.remap(registry.getValue(newKey));
        }
    }

    /** Block entity ids are saved in the chunk data, so they are fixed while loading (BlockEntityLegacyIdMixin). */
    public static CompoundTag fixBlockEntityTag(CompoundTag tag) {
        ResourceLocation id = ResourceLocation.tryParse(tag.getString("id"));
        if (id == null) return tag;
        if (SNOW.equals(id.getNamespace())) {
            tag.putString("id", ImmersiveNature.MOD_ID + ":" + SNOW_NAMES.getOrDefault(id.getPath(), id.getPath()));
        } else if (WEATHERING.equals(id.getNamespace())) {
            tag.putString("id", ImmersiveNature.MOD_ID + ":" + id.getPath());
        }
        return tag;
    }
}
