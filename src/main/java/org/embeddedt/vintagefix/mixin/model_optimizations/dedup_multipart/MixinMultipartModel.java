package org.embeddedt.vintagefix.mixin.model_optimizations.dedup_multipart;

import net.minecraft.client.renderer.block.model.MultipartBakedModel;
import org.embeddedt.vintagefix.annotation.ClientOnlyMixin;
import org.spongepowered.asm.mixin.Mixin;

/**
 * TODO: implement selector cache from modern
 */
@SuppressWarnings({"UnresolvedMixinReference", "SynchronizationOnLocalVariableOrMethodParameter"})
@Mixin(value = MultipartBakedModel.class, priority = 1100)
@ClientOnlyMixin
public class MixinMultipartModel {
    /*
    @Redirect(
            method = {
                    "getQuads", // Mapped name in MCP, Moj and Yarn
                    "method_4707", // Intermediary
                    "emitBlockQuads", // Added by FRAPI, also needs to be synchronized
                    "getSelectors" // Forge moves the logic into its own method
            },
            at = @At(value = "INVOKE", target = "Ljava/util/Map;get(Ljava/lang/Object;)Ljava/lang/Object;"),
            remap = false
    )
    public <K, V> V redirectCacheGet(Map<K, V> map, K key) {
        synchronized (map) {
            return map.get(key);
        }
    }

    @Redirect(
            method = {"getQuads", "method_4707", "emitBlockQuads", "getSelectors"},
            at = @At(value = "INVOKE", target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"),
            remap = false
    )
    public <K, V> V redirectCachePut(Map<K, V> map, K key, V value) {
        synchronized (map) {
            return map.put(key, value);
        }
    }
    */
}
