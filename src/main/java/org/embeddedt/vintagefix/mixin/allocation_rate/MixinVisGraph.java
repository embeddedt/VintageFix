package org.embeddedt.vintagefix.mixin.allocation_rate;

import it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue;
import net.minecraft.client.renderer.chunk.VisGraph;
import net.minecraft.util.EnumFacing;
import org.embeddedt.vintagefix.annotation.ClientOnlyMixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.BitSet;
import java.util.EnumSet;
import java.util.Set;

@Mixin(VisGraph.class)
@ClientOnlyMixin
public abstract class MixinVisGraph {
    @Shadow
    @Final
    private BitSet bitSet;

    @Shadow
    protected abstract void addEdges(int pos, Set<EnumFacing> p_178610_2_);

    @Shadow
    protected abstract int getNeighborIndexAtFace(int pos, EnumFacing facing);

    /**
     * @author embeddedt
     * @reason use Fastutil objects to avoid integer boxing/unboxing, cache queue to avoid repeated capacity increase
     */
    @Overwrite
    private Set<EnumFacing> floodFill(int pos)
    {
        Set<EnumFacing> set = EnumSet.noneOf(EnumFacing.class);
        IntArrayFIFOQueue queue = new IntArrayFIFOQueue();
        queue.enqueue(pos);
        this.bitSet.set(pos, true);

        while (!queue.isEmpty())
        {
            int i = queue.dequeueInt();
            this.addEdges(i, set);

            for (EnumFacing enumfacing : EnumFacing.VALUES)
            {
                int j = this.getNeighborIndexAtFace(i, enumfacing);

                if (j >= 0 && !this.bitSet.get(j))
                {
                    this.bitSet.set(j, true);
                    queue.enqueue(j);
                }
            }
        }

        return set;
    }
}
