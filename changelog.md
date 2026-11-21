# Immersive Nature changelog

## 1.3.0
### Performance
- Weathering and growth area checks no longer create temporary position lists and Random objects. Area offsets are cached per size and reused with per-thread buffers, with the same order and results as before. Checks are about 2x faster and allocate ~99% less memory.

### Optimized
- The jar is about 14% smaller (3.08 MB -> 2.65 MB):
  - Snow-covered blocks are drawn with the plain Forge model API, so the bundled Fabric renderer libraries (fabric-api-base, fabric-renderer-api, fabric-renderer-indigo, ~195 KB) are gone. The blocks look the same.
  - 25 unused textures removed (old branch textures, `snow_bricks_alternate`, `ice_sickle_legacy`, `edge_grass`).
  - PNG textures are recompressed without metadata Minecraft never reads (color profiles, DPI). The build also does this for every PNG it packs. Pixels are unchanged.

### Fixed
- Dynamic Trees compat: thin mangrove root blocks under the trunk were drawn black. They copied the light blocking of a full mangrove log, so no light reached their sides. Thin root blocks now let light through, and already generated ones are relit automatically.

### Changed
- Rain also raises lakes, rivers and oceans, so long rain can flood their shores. New `rain_water.floods` option (on by default); turn it off to let rain only fill puddles.
- Snow (Snow! Real Magic) and fluids (Flowing Fluids) are no longer separate mods inside the jar: everything is one mod, `immersive_nature`. Old worlds are converted automatically: `snowrealmagic:*` blocks and block entities load as `immersive_nature:*` (with clearer names, e.g. `snowy_fence`, `snow_layer`), and so do the old `immersive_weathering:*` ids, which were not remapped correctly before. The separate Snow! Real Magic and Flowing Fluids mods are marked incompatible.
- One config file: `config/immersive_nature-common.toml` now also holds the `overgrowth`, `snow` and `fluids` sections (before: a per-world overgrowth config, two snow YAML files and a fluids JSON file). Graphics settings, including snow, are in `immersive_nature-client.toml`. Changes made with `/flowing_fluids` are saved into the same file. Old separate config files are no longer read.
- Biomes O' Plenty: flowering oak leaves get oak leaf pile textures; the "null" leaves no longer get a leaf pile.
- All texts are in the mod's language files (assets/immersive_nature/lang): the /flowing_fluids command texts, the fluids join message and the rain water command are now translatable.
- Translations: English, Russian, Ukrainian, German, French, Spanish, Portuguese (Brazil) and Chinese (Simplified). Missing lines and other languages fall back to English.
- Overgrowth: the nylium spread setting now controls nylium spreading (it used the mud drying value).

### Other
- Code moved to the `com.immersive_nature` package, classes and mixin configs renamed to Immersive Nature, creative tab renamed to Immersive Nature.

## 1.2.0
### Added
- Desire Paths: grass and podzol slowly wear into dirt, then coarse dirt, where players and mobs walk. Each block remembers how much it was walked on and forgets after a while.
- Water soaking: standing water soaks into the ground, turning grass into dirt, coarse dirt or mud, and dirt into mud, each with its own configurable chance.
- Grass can sprout on bare grass blocks, and other plants spread from it.
- Lily pads pop off when there is no water under them.

### Changed
- Rain water places a configurable number of Flowing Fluids water levels (default 4) and tops up existing puddles; radius configurable (default 20).
- Wet earthen clay leaves a configurable number of water levels when broken (default 4).
- Falling icicles shatter with sound and particles.
- Overgrowth's entity paths are replaced by Desire Paths.

### Performance
- Overgrowth skips air-only chunk sections, block growths skip blocks without rules early, water erosion works out the flow once per tick, unused code and files removed, textures recompressed, JSON minified in the jar.
