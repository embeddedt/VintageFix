package org.embeddedt.vintagefix.mixin.texture_animation;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.WorldVertexBufferUploader;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.embeddedt.vintagefix.annotation.ClientOnlyMixin;
import org.embeddedt.vintagefix.render.ExtendedBufferBuilderState;
import org.embeddedt.vintagefix.render.ExtendedTextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;

@Mixin(WorldVertexBufferUploader.class)
@ClientOnlyMixin
public class MixinWorldVertexBufferUploader {
    @Inject(method = "draw", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/BufferBuilder;getVertexFormat()Lnet/minecraft/client/renderer/vertex/VertexFormat;"))
    private void activateTextures(BufferBuilder builder, CallbackInfo ci) {
        Collection<TextureAtlasSprite> sprites = ((ExtendedBufferBuilderState)builder).vfix$getAnimatedSprites();
        if(!sprites.isEmpty()) {
            for(TextureAtlasSprite sprite : sprites) {
                ((ExtendedTextureAtlasSprite)sprite).vfix$setActive(true);
            }
        }
    }
}
