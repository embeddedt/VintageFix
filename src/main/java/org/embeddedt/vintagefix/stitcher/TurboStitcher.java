package org.embeddedt.vintagefix.stitcher;

import com.google.common.collect.ImmutableSet;
import net.minecraft.client.renderer.texture.Stitcher;
import net.minecraft.util.ResourceLocation;
import org.embeddedt.vintagefix.VintageFix;
import org.embeddedt.vintagefix.dynamicresources.TextureCollector;
import org.embeddedt.vintagefix.stitcher.packing2d.Algorithm;
import org.embeddedt.vintagefix.stitcher.packing2d.Packer;

import java.util.*;

public class TurboStitcher extends SpriteSlot {
    private final int maxWidth;
    private final int maxHeight;
    private final boolean forcePowerOf2;
    private List<SpriteSlot> slots = new LinkedList<>();
    private List<SpriteSlot> finalizedSlots = null;
    private boolean needsSorting = false;
    private int trackedArea = 0;
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
        trackedArea += rect.width * rect.height;
        needsSorting = true;
    }

    public void reset() {
        state = StitcherState.SETUP;
    }

    public void dropFirst() {
        verifyState(StitcherState.SETUP);
        if(slots.size() > 0) {
            SpriteSlot slot = slots.remove(0);
            String name;
            if (slot instanceof HolderSlot) {
                name = ((HolderSlot) slot).getHolder().getAtlasSprite().getIconName();
            } else {
                name = "unknown";
            }
            VintageFix.LOGGER.warn("Dropping {}x{} texture '{}' from atlas as it's too large", slot.width, slot.height, name);
            trackedArea -= slot.width * slot.height;
        } else
            throw new IllegalStateException();
    }

    public void retainAllSprites(Set<ResourceLocation> theLocations) {
        verifyState(StitcherState.SETUP);
        Set<String> locationStrings = new HashSet<>();
        for(ResourceLocation rl : theLocations)
            locationStrings.add(rl.toString());
        slots.removeIf(slot -> {
            if(slot instanceof HolderSlot) {
                String spriteName = ((HolderSlot) slot).getHolder().getAtlasSprite().getIconName();
                // drop textures that are:
                // - weakly collected by us
                // - not in the desired list of locations
                if(TextureCollector.weaklyCollectedTextures.contains(spriteName) && !locationStrings.contains(spriteName)) {
                    VintageFix.LOGGER.warn("Dropping unreferenced sprite " + ((HolderSlot) slot).getHolder().getAtlasSprite().getIconName());
                    trackedArea -= slot.width * slot.height;
                    return true;
                }
            }
            return false;
        });
    }

    public void stitch() throws TooBigException {
        verifyState(StitcherState.SETUP);
        width = 0;
        height = 0;
        if (slots.size() == 0) {
            state = StitcherState.STITCHED;
            return;
        }
        // ensure we have largest sprites first
        if(needsSorting) {
            Collections.sort(slots);
            needsSorting = false;
        }
        if(trackedArea > (maxWidth * maxHeight)) {
            throw new TooBigException();
        }
        // start with a really simple check, if the total area is larger than we could handle, we know this will fail
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
        List<SpriteSlot> toPack = new ArrayList<>(slots);
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
            packedSlots = Packer.pack(toPack, Algorithm.FIRST_FIT_DECREASING_HEIGHT, width);
            height = 0;
            for (SpriteSlot sprite : packedSlots) {
                height = Math.max(height, sprite.y + sprite.height);
            }
            if (forcePowerOf2) {
                height = nextPowerOfTwo(height);
            }
        } while (height > maxHeight || height > width);
        finalizedSlots = packedSlots;
        state = StitcherState.STITCHED;
        TextureCollector.weaklyCollectedTextures = ImmutableSet.of();
    }

    public List<Stitcher.Slot> getSlots() {
        return getSlots(new Rect2D());
    }

    public List<Stitcher.Slot> getSlots(Rect2D parent) {
        verifyState(StitcherState.STITCHED);
        ArrayList<Stitcher.Slot> mineSlots = new ArrayList<Stitcher.Slot>();
        Rect2D offset = new Rect2D(x + parent.x, y + parent.y, width, height);
        for (SpriteSlot slot : finalizedSlots) {
            mineSlots.addAll(slot.getSlots(offset));
        }
        /*
        for(Stitcher.Slot slot : mineSlots) {
            System.out.println(slot.getStitchHolder().getAtlasSprite().getIconName());
        }

         */
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
