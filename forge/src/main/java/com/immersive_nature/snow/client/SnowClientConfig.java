package com.immersive_nature.snow.client;

import net.minecraft.client.Minecraft;
import snownee.kiwi.config.KiwiConfig;
import snownee.kiwi.config.KiwiConfig.ConfigType;

@KiwiConfig(value = "immersive_nature-snow-client", type = ConfigType.CLIENT)
public final class SnowClientConfig {

	public static boolean particleThroughLeaves = true;

	public static boolean snowVariants = true;

	@KiwiConfig.Listen("snowVariants")
	public static void toggleSnowVariants(String key) {
		if (Minecraft.getInstance().level != null) {
			Minecraft.getInstance().levelRenderer.allChanged();
		}
	}
}
