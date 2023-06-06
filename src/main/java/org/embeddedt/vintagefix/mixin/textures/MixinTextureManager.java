package org.embeddedt.vintagefix.mixin.textures;

import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import org.embeddedt.vintagefix.VintageFix;
import org.embeddedt.vintagefix.annotation.ClientOnlyMixin;
import org.embeddedt.vintagefix.stitcher.IAsyncTexture;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(TextureManager.class)
@ClientOnlyMixin
public class MixinTextureManager {
    @Shadow
    @Final
    private Map<ResourceLocation, ITextureObject> mapTextureObjects;

    @Inject(method = "onResourceManagerReload", at = @At(value = "HEAD"))
    private void submitAsyncReloads(IResourceManager manager, CallbackInfo ci) {
        for(ITextureObject object : this.mapTextureObjects.values()) {
            if(object instanceof IAsyncTexture && object != TextureUtil.MISSING_TEXTURE) {
                ((IAsyncTexture)object).runAsyncLoadPortion(manager, VintageFix.WORKER_POOL);
            }
        }
    }
}
