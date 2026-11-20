package com.immersive_nature.snow.compat.jade;

import static com.immersive_nature.snow.CoreModule.FENCE;
import static com.immersive_nature.snow.CoreModule.FENCE2;
import static com.immersive_nature.snow.CoreModule.FENCE_GATE;
import static com.immersive_nature.snow.CoreModule.SLAB;
import static com.immersive_nature.snow.CoreModule.STAIRS;
import static com.immersive_nature.snow.CoreModule.TILE_BLOCK;
import static com.immersive_nature.snow.CoreModule.WALL;

import java.util.List;

import net.minecraft.world.level.block.Block;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.kiwi.KiwiGO;

@WailaPlugin
public class JadeCompat implements IWailaPlugin {

	@Override
	public void registerClient(IWailaClientRegistration registration) {
		for (KiwiGO<? extends Block> block : List.of(TILE_BLOCK, FENCE, FENCE2, STAIRS, SLAB, FENCE_GATE, WALL))
			registration.usePickedResult(block.get());
	}

}
