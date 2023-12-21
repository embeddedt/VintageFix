package org.embeddedt.vintagefix.dynamicresources.model;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceLinkedOpenHashMap;
import net.minecraft.client.renderer.block.model.IBakedModel;

import java.util.concurrent.locks.StampedLock;
import java.util.function.Function;

/**
 * The Mojang Triple-based baked cache system is too slow to be hitting on every model retrieval, so
 * we need a fast, concurrency-safe wrapper on top.
 */
public class DynamicModelCache<K> {
    private final Reference2ReferenceLinkedOpenHashMap<K, IBakedModel> cache = new Reference2ReferenceLinkedOpenHashMap<>();
    private final StampedLock lock = new StampedLock();
    private final Function<K, IBakedModel> modelRetriever;
    private final boolean allowNulls;

    public DynamicModelCache(Function<K, IBakedModel> modelRetriever, boolean allowNulls) {
        this.modelRetriever = modelRetriever;
        this.allowNulls = allowNulls;
    }

    public void clear() {
        long stamp = lock.writeLock();
        try {
            cache.clear();
        } finally {
            lock.unlock(stamp);
        }
    }

    private boolean needToPopulate(K state) {
        long stamp = lock.readLock();
        try {
            return !cache.containsKey(state);
        } finally {
            lock.unlock(stamp);
        }
    }

    private IBakedModel getModelFromCache(K state) {
        long stamp = lock.readLock();
        try {
            return cache.get(state);
        } finally {
            lock.unlock(stamp);
        }
    }

    private IBakedModel cacheModel(K state) {
        IBakedModel model = modelRetriever.apply(state);

        // Lock and modify our local, faster cache
        long stamp = lock.writeLock();

        try {
            cache.putAndMoveToFirst(state, model);
            // TODO: choose less arbitrary number
            if(cache.size() >= 1000) {
                cache.removeLast();
            }
        } finally {
            lock.unlock(stamp);
        }

        return model;
    }

    public IBakedModel get(K key) {
        IBakedModel model = getModelFromCache(key);

        if(model == null && (!allowNulls || needToPopulate(key))) {
            model = cacheModel(key);
        }

        return model;
    }
}
