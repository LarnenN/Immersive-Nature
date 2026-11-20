// Flowing Fluids by Traben (https://github.com/Traben-0/flowing_fluids), LGPL-3.0. Built into Immersive Nature.
package com.immersive_nature.fluids.mixin.mixins.create;

import com.simibubi.create.content.fluids.transfer.FluidDrainingBehaviour;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;

@Pseudo
@Mixin({FluidDrainingBehaviour.class})
public interface FluidDrainingBehaviourAccessor {
   @Accessor("fluid")
   Fluid ff$getFluid();

   @Accessor("fluid")
   void ff$setFluid(Fluid var1);
}
