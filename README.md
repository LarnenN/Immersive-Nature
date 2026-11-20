# Immersive Nature

Forge 1.20.1 mod that makes the world feel alive: weathering, falling leaves, ash, water erosion and nature slowly reclaiming the land.

## Built from
| Mod | Author | License | What was taken |
|---|---|---|---|
| [Immersive Weathering](https://github.com/Silversmith-Mods/Immersive-Weathering) | Ordana, MehVahdJukaar, Keybounce | LGPL-3.0 | base of the mod |
| [Water Erosion](https://github.com/13rac1/Minecraft-Water-Erosion) | 13rac1 | GPL-3.0 | water erosion (`erosion` package) |
| [Overgrowth](https://github.com/ldtteam/Overgrowth) | ldtteam | GPL-3.0 (per its mods.toml) | world decay/overgrowth handlers (`forge/overgrowth`) |
| [Snow! Real Magic](https://github.com/Snownee/SnowRealMagic) | Snownee | MIT | whole mod, merged into mod id `immersive_nature` (`com.immersive_nature.snow`) |
| [Flowing Fluids](https://github.com/Traben-0/flowing_fluids) | Traben | LGPL-3.0 | whole mod 1.0.8, merged into mod id `immersive_nature` (`com.immersive_nature.fluids`) |

Own reimplementation (no code copied): rain water gathering on the ground, an idea from
[Rain Water Fixed](https://www.curseforge.com/minecraft/mc-mods/rain-water-fixed) by Link15 (`forge/rainwater`),
and desire paths (grass wearing down where you walk), an idea from
[Desire Paths](https://www.curseforge.com/minecraft/mc-mods/desire-path) by Insane96MCP (`forge/desirepaths`).

Optional compatibility (no code copied): Dynamic Trees.
Required separately: Moonlight Lib, Kiwi.

## License
GPL-3.0-or-later, including all original code (see `LICENSE`). Original licenses of the included mods are in `licenses/`.
Modified files carry a note saying where they come from.
