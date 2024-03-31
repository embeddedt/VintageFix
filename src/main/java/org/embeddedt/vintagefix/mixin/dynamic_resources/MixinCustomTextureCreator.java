package org.embeddedt.vintagefix.mixin.dynamic_resources;

import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanMaps;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import net.minecraft.client.resources.IResourceManager;
import org.embeddedt.vintagefix.annotation.ClientOnlyMixin;
import org.embeddedt.vintagefix.annotation.LateMixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import slimeknights.tconstruct.library.client.CustomTextureCreator;

@Mixin(value = CustomTextureCreator.class)
@ClientOnlyMixin
@LateMixin
public class MixinCustomTextureCreator {
    private static final Object2BooleanMap<String> textureExistenceCache = Object2BooleanMaps.synchronize(new Object2BooleanOpenHashMap<>());

    @Inject(method = "exists", at = @At("HEAD"), cancellable = true, remap = false)
    private static void checkCache(String key, CallbackInfoReturnable<Boolean> cir) {
        Boolean result = textureExistenceCache.get(key);
        if(result != null) {
            cir.setReturnValue(result);
        }
    }

    @Inject(method = "exists", at = @At("RETURN"), remap = false)
    private static void storeCache(String key, CallbackInfoReturnable<Boolean> cir) {
        textureExistenceCache.put(key, cir.getReturnValue());
    }

    @Inject(method = "onResourceManagerReload", at = @At("HEAD"))
    private void clearCache(IResourceManager resourceManager, CallbackInfo ci) {
        textureExistenceCache.clear();
    }
}
