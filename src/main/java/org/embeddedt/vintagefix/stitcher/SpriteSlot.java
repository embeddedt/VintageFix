package org.embeddedt.vintagefix.stitcher;

import net.minecraft.client.renderer.texture.Stitcher;

import java.util.List;

public abstract class SpriteSlot extends Rect2D {
    public abstract List<Stitcher.Slot> getSlots(Rect2D parent);
}
