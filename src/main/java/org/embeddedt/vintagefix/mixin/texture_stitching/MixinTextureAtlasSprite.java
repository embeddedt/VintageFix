package org.embeddedt.vintagefix.mixin.texture_stitching;

import net.minecraft.client.renderer.texture.PngSizeInfo;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.data.AnimationMetadataSection;
import org.embeddedt.vintagefix.stitcher.TextureCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

@Mixin(TextureAtlasSprite.class)
public class MixinTextureAtlasSprite {
    @Shadow
    protected int width;

    @Shadow
    protected int height;

    @Inject(method = "loadSprite", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;resetSprite()V", shift = At.Shift.AFTER), cancellable = true)
    private void skipEarlyLoad(PngSizeInfo info, boolean flag, CallbackInfo ci) {
        if(info == null) {
            /* put a sensible default width/height */
            this.width = 16;
            this.height = 16;
            ci.cancel();
        }
    }

    @Redirect(method = "loadSpriteFrames", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/texture/TextureUtil;readBufferedImage(Ljava/io/InputStream;)Ljava/awt/image/BufferedImage;"))
    private BufferedImage useCachedImage(InputStream stream, IResource resource) throws IOException {
        BufferedImage image = TextureCache.textureLoadingCache.get(resource.getResourceLocation());
        if(image != null)
            return image;
        else {
            return TextureUtil.readBufferedImage(stream);
        }
    }

    @Inject(method = "loadSpriteFrames", at = @At(value = "INVOKE", target = "Ljava/awt/image/BufferedImage;getRGB(IIII[III)[I", ordinal = 0), locals = LocalCapture.CAPTURE_FAILHARD)
    private void onSpriteFramesLoad(IResource resource, int mipmaplevels, CallbackInfo ci, BufferedImage image, AnimationMetadataSection meta) {
        this.width = image.getWidth();
        this.height = image.getHeight();
        if(meta != null)
            this.height = this.width; /* is animation, correct height */
        else if(this.height != this.width) {
            throw new RuntimeException("broken aspect ratio and not an animation");
        }
    }
}
