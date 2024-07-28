package org.embeddedt.vintagefix.util;

import net.minecraft.util.math.BlockPos;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class SectionBlockPosIterator implements Iterator<BlockPos> {
    private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
    private int index = 0;
    private final int baseX, baseY, baseZ;

    public SectionBlockPosIterator(int baseX, int baseY, int baseZ) {
        this.baseX = baseX;
        this.baseY = baseY;
        this.baseZ = baseZ;
    }

    public SectionBlockPosIterator(BlockPos pos) {
        this(pos.getX(), pos.getY(), pos.getZ());
    }

    @Override
    public boolean hasNext() {
        return index < 4096;
    }

    @Override
    public BlockPos next() {
        int i = index;
        if (i >= 4096) {
            throw new NoSuchElementException();
        }
        index = i + 1;
        BlockPos.MutableBlockPos pos = this.pos;
        pos.setPos(this.baseX + (i & 15), this.baseY + ((i >> 8) & 15), this.baseZ + ((i >> 4) & 15));
        return pos;
    }
}
