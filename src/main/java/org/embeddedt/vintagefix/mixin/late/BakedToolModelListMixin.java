package org.embeddedt.vintagefix.mixin.late;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.minecraft.client.renderer.block.model.IBakedModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.TimeUnit;

@Mixin(targets = "slimeknights/tconstruct/library/client/model/BakedToolModel$ToolItemOverrideList")
public class BakedToolModelListMixin {
    @Shadow
    private Cache<?, IBakedModel> bakedModelCache;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void useSoftCache(CallbackInfo ci) {
        this.bakedModelCache = CacheBuilder.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(3, TimeUnit.MINUTES)
            .softValues()
            .build();
    }
}
