package org.embeddedt.vintagefix.mixin.textures;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.data.TextureMetadataSection;
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

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Mixin(SimpleTexture.class)
@ClientOnlyMixin
public abstract class MixinSimpleTexture extends AbstractTexture implements IAsyncTexture {
    @Shadow
    @Final
    protected ResourceLocation textureLocation;
    private CompletableFuture<Void> asyncTextureLoad = null;
    private int[][] imageData;
    private int width, height;
    private boolean blur, clamp;
    @Override
    public void runAsyncLoadPortion(IResourceManager manager, Executor executor) {
        asyncTextureLoad = CompletableFuture.runAsync(() -> {
            try(IResource resource = manager.getResource(this.textureLocation)) {
                BufferedImage bufferedImage = TextureUtil.readBufferedImage(resource.getInputStream());
                width = bufferedImage.getWidth();
                height = bufferedImage.getHeight();
                int[][] data = new int[1][width * height];
                bufferedImage.getRGB(0, 0, width, height, data[0], 0, width);
                imageData = data;
                blur = false;
                clamp = false;
                if(resource.hasMetadata()) {
                    try {
                        TextureMetadataSection texturemetadatasection = (TextureMetadataSection)resource.getMetadata("texture");

                        if (texturemetadatasection != null) {
                            blur = texturemetadatasection.getTextureBlur();
                            clamp = texturemetadatasection.getTextureClamp();
                        }
                    } catch(RuntimeException ignored) {
                    }
                }
            } catch(IOException | RuntimeException e) {
                imageData = null;
                VintageFix.LOGGER.error("Exception reading texture", e);
            }
        }, executor);
    }

    @Inject(method = "loadTexture", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/texture/SimpleTexture;deleteGlTexture()V", shift = At.Shift.AFTER), cancellable = true)
    private void loadFast(IResourceManager resourceManager, CallbackInfo ci) {
        if(this.asyncTextureLoad != null) {
            this.asyncTextureLoad.join();
            this.asyncTextureLoad = null;
            if(this.imageData != null) {
                TextureUtil.allocateTexture(this.getGlTextureId(), width, height);
                GlStateManager.bindTexture(this.getGlTextureId());
                TextureUtil.uploadTextureMipmap(this.imageData, this.width, this.height, 0, 0, blur, clamp);
                ci.cancel();
            }
        }
    }
}
