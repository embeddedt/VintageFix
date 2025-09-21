package org.embeddedt.vintagefix.impl.model;

import com.google.common.collect.ImmutableList;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.util.EnumFacing;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A more efficient backing store for sided quad lists, inspired by how modern vanilla versions store them.
 * Name shamelessly borrowed from Omni's work on backporting models to 1.7.10.
 */
public final class PileOfQuads extends AbstractMap<EnumFacing, List<BakedQuad>> {
    private final List<BakedQuad> down, up, north, south, east, west;

    public PileOfQuads(Map<EnumFacing, List<BakedQuad>> original) {
        original = minimizeLists(original);
        this.down = original.getOrDefault(EnumFacing.DOWN, ImmutableList.of());
        this.up = original.getOrDefault(EnumFacing.UP, ImmutableList.of());
        this.north = original.getOrDefault(EnumFacing.NORTH, ImmutableList.of());
        this.south = original.getOrDefault(EnumFacing.SOUTH, ImmutableList.of());
        this.east = original.getOrDefault(EnumFacing.EAST, ImmutableList.of());
        this.west = original.getOrDefault(EnumFacing.WEST, ImmutableList.of());
    }

    private static Map<EnumFacing, List<BakedQuad>> minimizeLists(Map<EnumFacing, List<BakedQuad>> map) {
        // Step 1: Count total size and build main list
        ImmutableList.Builder<BakedQuad> mainBackingList = ImmutableList.builder();
        int[] offsets = new int[EnumFacing.VALUES.length];

        int totalSize = 0;
        for (int i = 0; i < EnumFacing.VALUES.length; i++) {
            List<BakedQuad> quads = map.getOrDefault(EnumFacing.VALUES[i], ImmutableList.of());
            offsets[i] = totalSize;
            mainBackingList.addAll(quads);
            totalSize += quads.size();
        }

        ImmutableList<BakedQuad> fullList = mainBackingList.build();

        // Step 2: Build the result map with sublists
        Map<EnumFacing, List<BakedQuad>> result = new EnumMap<>(EnumFacing.class);

        for (int i = 0; i < EnumFacing.VALUES.length; i++) {
            int start = offsets[i];
            int end = start + map.getOrDefault(EnumFacing.VALUES[i], ImmutableList.of()).size();
            result.put(EnumFacing.VALUES[i], fullList.subList(start, end));
        }

        return result;
    }

    @Override
    public List<BakedQuad> get(Object key) {
        if (!(key instanceof EnumFacing)) {
            return null;
        }
        switch ((EnumFacing) key) {
            case DOWN: return down;
            case UP: return up;
            case NORTH: return north;
            case SOUTH: return south;
            case EAST: return east;
            case WEST: return west;
            default: return null;
        }
    }

    @Override
    public boolean containsKey(Object key) {
        return key instanceof EnumFacing;
    }

    @Override
    public Set<Entry<EnumFacing, List<BakedQuad>>> entrySet() {
        return new EntrySet();
    }

    private final class EntrySet extends AbstractSet<Map.Entry<EnumFacing, List<BakedQuad>>> {
        @Override
        public Iterator<Entry<EnumFacing, List<BakedQuad>>> iterator() {
            return new Iterator<Entry<EnumFacing, List<BakedQuad>>>() {
                private int index = 0;

                @Override
                public boolean hasNext() {
                    return index < EnumFacing.VALUES.length;
                }

                @Override
                public Entry<EnumFacing, List<BakedQuad>> next() {
                    EnumFacing face = EnumFacing.VALUES[index++];
                    return new AbstractMap.SimpleImmutableEntry<>(face, get(face));
                }
            };
        }

        @Override
        public int size() {
            return EnumFacing.VALUES.length;
        }
    }
}
