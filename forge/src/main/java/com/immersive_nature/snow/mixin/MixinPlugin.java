package com.immersive_nature.snow.mixin;

import java.util.List;
import java.util.Set;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import net.minecraftforge.fml.loading.LoadingModList;

public class MixinPlugin implements IMixinConfigPlugin {

	private boolean hasKiwi;

	private static boolean hasMod(String modid) {
		return LoadingModList.get().getModFileById(modid) != null;
	}

	@Override
	public void onLoad(String mixinPackage) {
		hasKiwi = hasMod("kiwi");
		// configs of the separate Snow! Real Magic mod (before it became part of Immersive Nature)
		moveConfig("snowrealmagic-common.yaml", "immersive_nature-snow.yaml");
		moveConfig("snowrealmagic-client.yaml", "immersive_nature-snow-client.yaml");
	}

	private static void moveConfig(String from, String to) {
		try {
			java.nio.file.Path dir = net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get();
			java.nio.file.Path old = dir.resolve(from), target = dir.resolve(to);
			if (java.nio.file.Files.exists(old) && !java.nio.file.Files.exists(target)) {
				java.nio.file.Files.move(old, target);
			}
		} catch (Exception ignored) {
		}
	}

	@Override
	public String getRefMapperConfig() {
		return null;
	}

	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		return hasKiwi;
	}

	@Override
	public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
	}

	@Override
	public List<String> getMixins() {
		return null;
	}

	@Override
	public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
	}

	@Override
	public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
	}

}
