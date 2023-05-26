package org.embeddedt.vintagefix.impl;

import com.google.common.collect.ImmutableList;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.util.EnumFacing;
import org.embeddedt.vintagefix.core.VintageFixCore;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Use immutable lists for all quad lists. The backing array will exactly match the list size, and all empty lists will
 * use the same list instance. Additionally, the shallow size will be smaller than that of an ArrayList (otherwise
 * ArrayList#trimToSize could be used)
 */
public class ModelSidesImpl {
    private static final EnumFacing[] SIDES = EnumFacing.values();
    private static final Map<EnumFacing, List<BakedQuad>> EMPTY;
    static {
        EnumMap<EnumFacing, List<BakedQuad>> map = new EnumMap<>(EnumFacing.class);
        for (EnumFacing side : SIDES) {
            map.put(side, ImmutableList.of());
        }
        EMPTY = map;
    }

    private static <T> List<T> safeImmutableList(List<T> quadList) {
        // OptiFine requires list to be mutable
        return VintageFixCore.OPTIFINE ? quadList : ImmutableList.copyOf(quadList);
    }

    public static List<BakedQuad> minimizeUnculled(List<BakedQuad> quads) {
        return safeImmutableList(quads);
    }

    public static Map<EnumFacing, List<BakedQuad>> minimizeCulled(Map<EnumFacing, List<BakedQuad>> quadsBySide) {
        if (quadsBySide.isEmpty()) {
            // Workaround: Forge's EmptyModel does this, I'm quite sure that it would crash if it was actually used
            // anywhere
            return quadsBySide;
        }
        boolean allEmpty = true;
        for (final EnumFacing face : SIDES) {
            final List<BakedQuad> sideQuads = quadsBySide.get(face);
            try {
                quadsBySide.put(face, safeImmutableList(sideQuads));
            } catch(RuntimeException e) {
                /* some models don't allow putting entries in this map */
            }
            allEmpty &= sideQuads.isEmpty();
        }
        return allEmpty ? EMPTY : quadsBySide;
    }
}
