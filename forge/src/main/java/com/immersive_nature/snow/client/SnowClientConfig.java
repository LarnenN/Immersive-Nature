package com.immersive_nature.snow.client;

import net.minecraft.client.Minecraft;

/** Snow graphics settings, filled from the "snow" section of the Immersive Nature client config (PlatformConfigs). */
public final class SnowClientConfig {

	public static boolean particleThroughLeaves = true;

	public static boolean snowVariants = true;

	/** Redraws the world after snow variants were switched on or off. */
	public static void reloadChunks() {
		if (Minecraft.getInstance().level != null) {
			Minecraft.getInstance().levelRenderer.allChanged();
		}
	}
}
