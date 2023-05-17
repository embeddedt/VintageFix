package org.embeddedt.vintagefix.stitcher;

import net.minecraft.client.renderer.texture.Stitcher;
import org.embeddedt.vintagefix.stitcher.packing2d.Algorithm;
import org.embeddedt.vintagefix.stitcher.packing2d.Packer;

import java.util.ArrayList;
import java.util.List;

public class TurboStitcher extends SpriteSlot {
    private final int maxWidth;
    private final int maxHeight;
    private final boolean forcePowerOf2;
    private List<SpriteSlot> slots = new ArrayList<>();
    private StitcherState state = StitcherState.SETUP;
    private static final boolean OPTIMAL_PACKING = true;

    public TurboStitcher(int maxWidth, int maxHeight, boolean forcePowerOf2) {
        this.maxHeight = maxHeight;
        this.maxWidth = maxWidth;
        this.forcePowerOf2 = forcePowerOf2;
    }

    private static int nextPowerOfTwo(int number) {
        number--;
        number |= number >>> 1;
        number |= number >>> 2;
        number |= number >>> 4;
        number |= number >>> 8;
        number |= number >>> 16;
        number++;
        return number;
    }

    public void addSprite(Stitcher.Holder holder) {
        addSprite(new HolderSlot(holder));
    }

    public void addSprite(SpriteSlot rect) {
        verifyState(StitcherState.SETUP);
        slots.add(rect);
    }

    public void reset() {
        slots = new ArrayList<>();
        state = StitcherState.SETUP;
    }

    public void stitch() throws TooBigException {
        verifyState(StitcherState.SETUP);
        width = 0;
        height = 0;
        if (slots.size() == 0) {
            state = StitcherState.STITCHED;
            return;
        }
        for (SpriteSlot slot : slots) {
            width = Math.max(width, slot.width);
        }
        if (forcePowerOf2 || !OPTIMAL_PACKING) {
            width = nextPowerOfTwo(width);
        }
        if (width > maxWidth) {
            throw new TooBigException();
        }
        width = Math.max(width >>> 1, 1);
        List<SpriteSlot> packedSlots;
        do {
            if (width == maxWidth) {
                throw new TooBigException();
            }
            if (forcePowerOf2 || !OPTIMAL_PACKING) {
                width *= 2;
            } else {
                width += Math.min(width, 16);
            }
            if (width > maxWidth) {
                width = maxWidth;
            }
            packedSlots = Packer.pack(slots, Algorithm.FIRST_FIT_DECREASING_HEIGHT, width);
            height = 0;
            for (SpriteSlot sprite : packedSlots) {
                height = Math.max(height, sprite.y + sprite.height);
            }
            if (forcePowerOf2) {
                height = nextPowerOfTwo(height);
            }
        } while (height > maxHeight || height > width);
        slots = packedSlots;
        state = StitcherState.STITCHED;
    }

    public List<Stitcher.Slot> getSlots() {
        return getSlots(new Rect2D());
    }

    public List<Stitcher.Slot> getSlots(Rect2D parent) {
        verifyState(StitcherState.STITCHED);
        ArrayList<Stitcher.Slot> mineSlots = new ArrayList<Stitcher.Slot>();
        Rect2D offset = new Rect2D(x + parent.x, y + parent.y, width, height);
        for (SpriteSlot slot : slots) {
            mineSlots.addAll(slot.getSlots(offset));
        }
        return mineSlots;
    }

    private void verifyState(StitcherState... allowedStates) {
        boolean ok = false;
        for (StitcherState state : allowedStates) {
            if (state == this.state) {
                ok = true;
                break;
            }
        }
        if (!ok) {
            throw new IllegalStateException("Cold not execute operation: invalid state");
        }
    }
}
