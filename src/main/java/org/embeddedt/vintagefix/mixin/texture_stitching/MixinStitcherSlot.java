package org.embeddedt.vintagefix.mixin.texture_stitching;

import net.minecraft.client.renderer.texture.Stitcher;
import org.embeddedt.vintagefix.stitcher.IStitcherSlotMixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Stitcher.Slot.class)
public abstract class MixinStitcherSlot implements IStitcherSlotMixin {
    @Shadow
    private Stitcher.Holder holder;

    @Override
    public void insertHolder(Stitcher.Holder holder) {
        this.holder = holder;
    }
}
