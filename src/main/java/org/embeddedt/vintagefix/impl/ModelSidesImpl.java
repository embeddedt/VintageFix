package org.embeddedt.vintagefix.impl;

import com.google.common.collect.ImmutableList;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.util.EnumFacing;
import org.embeddedt.vintagefix.core.VintageFixCore;
import org.embeddedt.vintagefix.impl.model.PileOfQuads;

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

    public static List<BakedQuad> minimizeUnculled(List<BakedQuad> quads) {
        return VintageFixCore.OPTIFINE ? quads : ImmutableList.copyOf(quads);
    }

    public static Map<EnumFacing, List<BakedQuad>> minimizeCulled(Map<EnumFacing, List<BakedQuad>> quadsBySide) {
        if (VintageFixCore.OPTIFINE || quadsBySide.isEmpty()) {
            return quadsBySide;
        }
        boolean allEmpty = true;
        for (final EnumFacing face : SIDES) {
            final List<BakedQuad> sideQuads = quadsBySide.get(face);
            allEmpty = sideQuads.isEmpty();
            if (!allEmpty) {
                break;
            }
        }
        return allEmpty ? EMPTY : new PileOfQuads(quadsBySide);
    }
}
