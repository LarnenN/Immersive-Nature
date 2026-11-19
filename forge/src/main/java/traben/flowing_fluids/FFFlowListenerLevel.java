// Flowing Fluids by Traben (https://github.com/Traben-0/flowing_fluids), LGPL-3.0. Built into Immersive Nature.
package traben.flowing_fluids;

import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;

public interface FFFlowListenerLevel {
   Map<BlockPos, Set<BlockPos>> ff$getFlowListenerPositions();
}
