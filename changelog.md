# Immersive Nature changelog

## 1.4.0
### Added
- Desire paths: a full sand block counts as 8 layers. Walking over sand or red sand presses it into a pile of 7 layers, and the pile keeps losing one layer at a time until the last layer is gone. A sand or red sand pile that reaches 8 layers turns into a full sand block.
- Desire paths: grassy silt wears down into silt and grassy sandy dirt into sandy dirt; grass can grow back on them later.
- Every desire path has its own wear speed in the config (`desire_paths`): `grass_block_wear_to_change`, `podzol_wear_to_change`, `dirt_wear_to_change`, `grassy_silt_wear_to_change`, `grassy_sandy_dirt_wear_to_change` (all 100, about 20 walks) and `sand_wear_to_change` (500, about 100 walks per layer); 0 turns one off. `wear_to_change` is now only for extra entries in `transformations`, which is empty by default. Also new: `sand_layers`, `sand_blocks` and `sand_min_layers`.
- Lit furnaces, smokers and blast furnaces leave soot on the wall above their opening and on the ceiling above their front. New option `charring.furnace_soot` (0.3 per random tick, 0 = off).
- Weather2 compat: Weather2 storms (rain, lightning, high wind and hail) count as rain under them, even when vanilla weather is clear: rain water puddles, Flowing Fluids rain refill, moss, mud, rain block growths and the other rain effects work there. Sandstorms pile up Immersive Nature sand piles instead of Weather2's own sand layer, and a pile of 8 layers becomes a sand block. New config section `weather2` (`storms_are_rain`, `sandstorms_make_sand_piles`).
- Blocks turn sandy 3 times faster during a sandstorm (a thunderstorm in a desert-like biome, or a Weather2 sandstorm) and snowy 3 times faster during a blizzard (a thunderstorm while it snows, or a Weather2 snowstorm). Weather2 sandstorms and snowstorms also make blocks sandy and snowy while vanilla weather is clear. New option `general.storm_sandy_snowy_multiplier`.
- Moss spreads 3 times faster while it rains on the block or right next to it. New option `mossy_blocks.rain_multiplier`.
- Dynamic Trees compat: burning branches can turn into charred branches of the same thickness instead of burning away. Charred branches stay connected to the rest of the tree. New config section `charring.dynamic_trees`: `branch_charring` on/off, the size groups `thin_max_radius` (3) and `medium_max_radius` (8), and a char chance for each group: `thin_char_chance` (0.1), `medium_char_chance` (0.05), `thick_char_chance` (0.01).

### Changed
- Lone grass plants sprout 5 times less often on grass blocks with no plants around (0.0001 -> 0.00002). New option `general.grass_seeding_chance` (0 = off, needs /reload).

### Fixed
- Grass blocks spread onto silt, sandy dirt, earthen clay, permafrost and rooted dirt again, turning them into their grassy versions. The built-in snow code replaced the grass spreading code and only knew plain dirt.
- Grass drowned under water turns into dirt, coarse dirt or mud by the `water_soaking` rules again (the same snow code always made plain dirt).
- Placing a snow layer by hand (snow item with no block data) crashed the game. Caused by a wrong check of the item data in 1.3.0.

## 1.3.0
### One mod
- Immersive Weathering, Water Erosion, Overgrowth, Snow! Real Magic and Flowing Fluids are now one mod with one mod id, `immersive_nature`. The separate Snow! Real Magic and Flowing Fluids mods are marked incompatible.
- Old worlds are converted automatically: `snowrealmagic:*` and `immersive_weathering:*` blocks, items and block entities load as `immersive_nature:*` (with clearer names, e.g. `snowy_fence`, `snow_layer`). The old `immersive_weathering:*` ids were not remapped correctly before.
- One config file: `config/immersive_nature-common.toml` holds all settings in sections (`overgrowth`, `snow`, `fluids` and the rest). Before, there was a per-world Overgrowth config, two snow YAML files and a fluids JSON file. Graphics settings, including snow, are in `immersive_nature-client.toml`. Changes made with `/flowing_fluids` are saved into the same file and keep its section order. Old separate config files are no longer read.
- New mod icon, and the mod list shows Immersive Nature with credits to the original authors.

### Added
- Rain floods: long rain also raises lakes, rivers and oceans, so their shores can flood. New `rain_water.floods` option (on by default); turn it off to let rain only fill puddles.
- Biomes O' Plenty: the Seasonal Forest maple leaf pile features place Immersive Nature leaf piles (1-3 layers) instead of BOP's own piles. Flowering oak leaves get oak leaf pile textures; the "null" leaves no longer get a leaf pile.
- Leaf piles of leaves from other mods can use hand-drawn textures: `textures/block/<mod id>/light_<type>_leaves.png`, `medium_...` and `heavy_...`.

### Translations
- All texts come from the mod's language files (`assets/immersive_nature/lang`). The `/flowing_fluids` command texts, the fluids join message and the rain water command are now translatable.
- Languages: English, Russian, Ukrainian, German, French, Spanish, Portuguese (Brazil) and Chinese (Simplified). Missing lines and other languages fall back to English.

### Fixed
- Dynamic Trees: the thin mangrove root blocks under the trunk were drawn black. They now let light through, never hold water (water already inside drains on a random tick), and already generated ones are relit automatically.
- Falling icicles really shatter now. Before, the icicle block was placed in the same tick it landed, before the shatter check could run.
- Overgrowth: the nylium spread setting now controls nylium spreading (it used the mud drying value).
- Snow game rule texts and the falling snow entity name are updated for the merged mod.

### Performance
- Weathering and growth area checks no longer create temporary position lists and Random objects. Area offsets are cached per size and reused with per-thread buffers, with the same order and results as before. Checks are about 2x faster and allocate ~99% less memory.

### Smaller jar
- The jar is about 14% smaller (3.08 MB -> 2.65 MB):
  - Snow-covered blocks are drawn with the plain Forge model API, so the bundled Fabric renderer libraries (fabric-api-base, fabric-renderer-api, fabric-renderer-indigo, ~195 KB) are gone. The blocks look the same.
  - 25 unused textures removed (old branch textures, `snow_bricks_alternate`, `ice_sickle_legacy`, `edge_grass`).
  - PNG textures are recompressed without metadata Minecraft never reads (color profiles, DPI, sRGB tags). The build also does this for every PNG it packs, and JSON files are minified. Pixels are unchanged.

### Other
- Code moved to the `com.immersive_nature` package; classes, mixin configs and the creative tab are renamed to Immersive Nature.
- Removed junk code: 90 unused imports, commented-out code, 8 config options that no code ever read, and an unused test class.
- The mod also starts in a development environment (runClient / runServer).

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
