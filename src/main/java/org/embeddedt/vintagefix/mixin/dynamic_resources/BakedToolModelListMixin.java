package org.embeddedt.vintagefix.mixin.dynamic_resources;

import com.google.common.cache.*;
import com.google.common.collect.ImmutableMap;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import org.embeddedt.vintagefix.annotation.ClientOnlyMixin;
import org.embeddedt.vintagefix.annotation.LateMixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Mixin(targets = "slimeknights/tconstruct/library/client/model/BakedToolModel$ToolItemOverrideList")
@ClientOnlyMixin
@LateMixin
public class BakedToolModelListMixin {
    @Shadow(remap = false)
    private Cache<?, IBakedModel> bakedModelCache;

    private LoadingCache<IBakedModel, Cache<Object, IBakedModel>> parentToRealCache = CacheBuilder.newBuilder()
        .maximumSize(70)
        .expireAfterWrite(3, TimeUnit.MINUTES)
        .weakKeys()
        .softValues()
        .build(new CacheLoader<IBakedModel, Cache<Object, IBakedModel>>() {
            @Override
            public Cache<Object, IBakedModel> load(IBakedModel key) throws Exception {
                return CacheBuilder.newBuilder()
                    .expireAfterWrite(3, TimeUnit.MINUTES)
                    .maximumSize(50)
                    .build();
            }
        });

    private static final MethodHandle parentModelGetter;

    static {
        Field parentField;
        MethodHandle m;
        try {
            Class<?> cacheKey = Class.forName("slimeknights.tconstruct.library.client.model.BakedToolModel$CacheKey");
            parentField = ObfuscationReflectionHelper.findField(cacheKey, "parent");
            parentField.setAccessible(true);
            m = MethodHandles.lookup().unreflectGetter(parentField);
        } catch(ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }

        parentModelGetter = m;
    }

    private IBakedModel cacheKeyToModel(Object key, Callable<? extends IBakedModel> loader) throws ExecutionException {
        IBakedModel parent;
        try {
            parent = (IBakedModel)parentModelGetter.invoke(key);
        } catch(Throwable e) {
            throw new ExecutionException(e);
        }
        Cache<Object, IBakedModel> realCache = parentToRealCache.get(parent);
        return realCache.get(key, loader);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void useSoftCache(CallbackInfo ci) {
        this.bakedModelCache = new Cache<Object, IBakedModel>() {
            @Nullable
            @Override
            public IBakedModel getIfPresent(Object key) {
                throw new UnsupportedOperationException();
            }

            @Override
            public IBakedModel get(Object key, Callable<? extends IBakedModel> loader) throws ExecutionException {
                return cacheKeyToModel(key, loader);
            }

            @Override
            public ImmutableMap<Object, IBakedModel> getAllPresent(Iterable<?> keys) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void put(Object key, IBakedModel value) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void putAll(Map<?, ? extends IBakedModel> m) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void invalidate(Object key) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void invalidateAll(Iterable<?> keys) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void invalidateAll() {
                throw new UnsupportedOperationException();
            }

            @Override
            public long size() {
                throw new UnsupportedOperationException();
            }

            @Override
            public CacheStats stats() {
                throw new UnsupportedOperationException();
            }

            @Override
            public ConcurrentMap<Object, IBakedModel> asMap() {
                throw new UnsupportedOperationException();
            }

            @Override
            public void cleanUp() {
                throw new UnsupportedOperationException();
            }
        };
    }
}
