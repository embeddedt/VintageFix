package org.embeddedt.vintagefix.mixin.invisible_subchunks;

import net.minecraft.client.renderer.chunk.SetVisibility;
import net.minecraft.util.EnumFacing;
import org.embeddedt.vintagefix.ducks.IPathingSetVisibility;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(SetVisibility.class)
public class MixinSetVisibility implements IPathingSetVisibility {
    @Shadow
    @Final
    private static int COUNT_FACES;

    private long bitField = 0;

    /**
     * @author Michael Kreitzer
     * @reason This class is everything that is wrong with Java
     */
    @Overwrite
    public void setVisible(EnumFacing fromFace, EnumFacing toFace, boolean isVisible)
    {
        long shiftBy;

        shiftBy = fromFace.ordinal() + toFace.ordinal() * COUNT_FACES;
        this.bitField = (this.bitField & ~(1L << shiftBy)) | ((isVisible ? 1L : 0L) << shiftBy);

        shiftBy = toFace.ordinal() + fromFace.ordinal() * COUNT_FACES;
        this.bitField = (this.bitField & ~(1L << shiftBy)) | ((isVisible ? 1L : 0L) << shiftBy);
    }

    /**
     * @author Michael Kreitzer
     * @reason This class is everything that is wrong with Java
     */
    @Overwrite
    public void setAllVisible(boolean visible)
    {
        this.bitField = visible ? 0xFFFFFFFFFL : 0L;
    }

    /**
     * @author Michael Kreitzer
     * @reason This class is everything that is wrong with Java
     */
    @Overwrite
    public boolean isVisible(EnumFacing fromFace, EnumFacing toFace)
    {
        boolean ret = ((1L << (fromFace.ordinal() + toFace.ordinal() * COUNT_FACES)) & this.bitField) != 0;
        return ret;
    }

    @Override
    public boolean vfix$anyPathToFace(EnumFacing targetFace)
    {
        long mask = 0b111111 << (targetFace.ordinal() * COUNT_FACES);
        return (mask & this.bitField) > 0;
    }
}
