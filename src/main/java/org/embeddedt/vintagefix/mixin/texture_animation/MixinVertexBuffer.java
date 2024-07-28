package org.embeddedt.vintagefix.mixin.texture_animation;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexBuffer;
import org.embeddedt.vintagefix.annotation.ClientOnlyMixin;
import org.embeddedt.vintagefix.render.ExtendedBufferBuilderState;
import org.embeddedt.vintagefix.render.ExtendedTextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;
import java.util.List;
import java.util.Set;

@Mixin(VertexBuffer.class)
@ClientOnlyMixin
public class MixinVertexBuffer implements ExtendedBufferBuilderState {
    private List<TextureAtlasSprite> vfix$sprites = ImmutableList.of();

    @Override
    public Collection<TextureAtlasSprite> vfix$getAnimatedSprites() {
        return vfix$sprites;
    }

    @Override
    public void vfix$setAnimatedSprites(Collection<TextureAtlasSprite> sprites) {
        vfix$sprites = !sprites.isEmpty() ? ImmutableList.copyOf(sprites) : ImmutableList.of();
    }

    @Inject(method = "drawArrays", at = @At("HEAD"))
    private void activateSprites(CallbackInfo ci) {
        Collection<TextureAtlasSprite> sprites = vfix$sprites;
        if(!sprites.isEmpty()) {
            for(TextureAtlasSprite sprite : sprites) {
                ((ExtendedTextureAtlasSprite)sprite).vfix$setActive(true);
            }
        }
    }
}
