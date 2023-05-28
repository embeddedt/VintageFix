package org.embeddedt.vintagefix.mixin.texture_stitching;

import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import org.embeddedt.vintagefix.annotation.ClientOnlyMixin;
import org.embeddedt.vintagefix.stitcher.IAsyncTexture;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Mixin(TextureManager.class)
@ClientOnlyMixin
public class MixinTextureManager {
    @Shadow
    @Final
    private Map<ResourceLocation, ITextureObject> mapTextureObjects;
    private static final Executor TEXTURE_RELOAD_EXECUTOR = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    @Inject(method = "onResourceManagerReload", at = @At(value = "HEAD"))
    private void submitAsyncReloads(IResourceManager manager, CallbackInfo ci) {
        for(ITextureObject object : this.mapTextureObjects.values()) {
            if(object instanceof IAsyncTexture && object != TextureUtil.MISSING_TEXTURE) {
                ((IAsyncTexture)object).runAsyncLoadPortion(manager, TEXTURE_RELOAD_EXECUTOR);
            }
        }
    }
}
