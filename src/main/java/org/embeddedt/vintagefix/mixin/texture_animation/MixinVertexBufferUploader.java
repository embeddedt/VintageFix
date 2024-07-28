package org.embeddedt.vintagefix.mixin.texture_animation;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.VertexBufferUploader;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexBuffer;
import org.embeddedt.vintagefix.annotation.ClientOnlyMixin;
import org.embeddedt.vintagefix.render.ExtendedBufferBuilderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;

@Mixin(VertexBufferUploader.class)
@ClientOnlyMixin
public class MixinVertexBufferUploader {
    @Shadow
    private VertexBuffer vertexBuffer;

    @Inject(method = "draw", at = @At("HEAD"))
    private void activateTextures(BufferBuilder builder, CallbackInfo ci) {
        Collection<TextureAtlasSprite> sprites = ((ExtendedBufferBuilderState)builder).vfix$getAnimatedSprites();
        ((ExtendedBufferBuilderState)this.vertexBuffer).vfix$setAnimatedSprites(sprites);
    }
}
