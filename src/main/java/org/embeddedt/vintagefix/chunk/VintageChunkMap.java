package org.embeddedt.vintagefix.chunk;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;

/**
 * This class implements a small LRU cache on top of Long2ObjectOpenHashMap to enable
 * faster chunk lookups if the same chunk is retrieved many times.
 */
public class VintageChunkMap extends Long2ObjectOpenHashMap<Chunk> {
    private static final boolean CACHE_ENABLED = false;

    private static final int CACHE_SIZE = 4;
    private final long[] cachedChunkPositions = new long[CACHE_SIZE];
    private final Chunk[] cachedChunks = new Chunk[CACHE_SIZE];

    public VintageChunkMap() {
        super(8192);
    }

    @Override
    public Chunk get(long key) {
        if (!CACHE_ENABLED) {
            return super.get(key);
        }

        for (int i = 0; i < 4; ++i) {
            // Consolidate the scan into one comparison, allowing the JVM to better optimize the function
            // This is considerably faster than scanning two arrays side-by-side
            if (key == cachedChunkPositions[i]) {
                Chunk chunk = this.cachedChunks[i];

                // If the chunk exists for the key, return the result
                // We also check that the position matches, in case mods try to access
                // chunks on the wrong thread
                if (chunk != null && ChunkPos.asLong(chunk.x, chunk.z) == key) {
                    return chunk;
                }
            }
        }

        Chunk chunk = super.get(key);

        if (chunk != null) {
            this.addToCache(key, chunk);
        }

        return chunk;
    }

    @Override
    public Chunk remove(long k) {
        if (CACHE_ENABLED) {
            for (int i = 0; i < 4; ++i) {
                if (k == cachedChunkPositions[i]) {
                    cachedChunks[i] = null;
                }
            }
        }

        return super.remove(k);
    }

    @Override
    protected void rehash(int newN) {
        // do not allow the backing table to ever shrink
        if (newN > this.key.length) {
            super.rehash(newN);
        }
    }

    private void addToCache(long key, Chunk chunk) {
        for (int i = CACHE_SIZE - 1; i > 0; --i) {
            this.cachedChunkPositions[i] = this.cachedChunkPositions[i - 1];
            this.cachedChunks[i] = this.cachedChunks[i - 1];
        }

        this.cachedChunkPositions[0] = key;
        this.cachedChunks[0] = chunk;
    }
}
