package org.embeddedt.vintagefix.mixin.textures;

import net.minecraft.client.renderer.texture.PngSizeInfo;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.data.AnimationMetadataSection;
import org.embeddedt.vintagefix.annotation.ClientOnlyMixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.awt.image.BufferedImage;

@Mixin(TextureAtlasSprite.class)
@ClientOnlyMixin
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
