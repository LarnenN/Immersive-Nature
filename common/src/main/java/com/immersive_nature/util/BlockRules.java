package com.immersive_nature.util;

import com.immersive_nature.ImmersiveNature;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Immersive Nature: block transformation rules read from a config list, as "from,to" or "from,to,chance".
 * "from" can be a block id or a #block_tag. Re-parsed only when the config list changes.
 */
public final class BlockRules {

    public record Rule(Block block, TagKey<Block> tag, Block result, double chance) {
        public boolean matches(BlockState state) {
            return tag != null ? state.is(tag) : state.is(block);
        }
    }

    private final String name;
    private final Supplier<List<String>> config;
    private final double defaultChance;
    private List<String> parsedFrom = null;
    private List<String> parsedFromRef = null;
    private List<Rule> rules = List.of();

    public BlockRules(String name, Supplier<List<String>> config, double defaultChance) {
        this.name = name;
        this.config = config;
        this.defaultChance = defaultChance;
    }

    public List<Rule> get() {
        List<String> lines = config.get();
        // same list object = config not reloaded, skip the line-by-line compare
        if (lines != parsedFromRef && !Objects.equals(lines, parsedFrom)) {
            List<Rule> parsed = new ArrayList<>();
            for (String line : lines) {
                Rule rule = parse(line);
                if (rule != null) parsed.add(rule);
            }
            rules = List.copyOf(parsed);
            parsedFrom = List.copyOf(lines);
        }
        parsedFromRef = lines;
        return rules;
    }

    private Rule parse(String line) {
        String[] parts = line.split(",");
        if (parts.length != 2 && parts.length != 3) {
            ImmersiveNature.LOGGER.warn("{}: ignoring \"{}\", expected \"from,to\" or \"from,to,chance\"", name, line);
            return null;
        }
        double chance = defaultChance;
        if (parts.length == 3) {
            try {
                chance = Double.parseDouble(parts[2].trim());
            } catch (NumberFormatException e) {
                ImmersiveNature.LOGGER.warn("{}: bad chance \"{}\" in \"{}\"", name, parts[2].trim(), line);
                return null;
            }
        }
        String from = parts[0].trim();
        Block to = block(parts[1].trim());
        if (to == null) {
            ImmersiveNature.LOGGER.warn("{}: unknown block \"{}\" in \"{}\"", name, parts[1].trim(), line);
            return null;
        }
        if (from.startsWith("#")) {
            ResourceLocation tagId = ResourceLocation.tryParse(from.substring(1));
            if (tagId == null) return null;
            return new Rule(null, TagKey.create(Registries.BLOCK, tagId), to, chance);
        }
        Block block = block(from);
        if (block == null) {
            ImmersiveNature.LOGGER.warn("{}: unknown block \"{}\" in \"{}\"", name, from, line);
            return null;
        }
        return new Rule(block, null, to, chance);
    }

    private static Block block(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        return rl == null ? null : BuiltInRegistries.BLOCK.getOptional(rl).orElse(null);
    }
}
