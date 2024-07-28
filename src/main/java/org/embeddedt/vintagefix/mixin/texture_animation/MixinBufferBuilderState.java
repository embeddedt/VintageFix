package org.embeddedt.vintagefix.mixin.texture_animation;

import com.google.common.collect.ImmutableList;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.embeddedt.vintagefix.annotation.ClientOnlyMixin;
import org.embeddedt.vintagefix.render.ExtendedBufferBuilderState;
import org.spongepowered.asm.mixin.Mixin;

import java.util.Collection;
import java.util.List;

@Mixin(BufferBuilder.State.class)
@ClientOnlyMixin
public class MixinBufferBuilderState implements ExtendedBufferBuilderState {
    private List<TextureAtlasSprite> vfix$animatedSprites = ImmutableList.of();

    @Override
    public Collection<TextureAtlasSprite> vfix$getAnimatedSprites() {
        return vfix$animatedSprites;
    }

    @Override
    public void vfix$setAnimatedSprites(Collection<TextureAtlasSprite> sprites) {
        vfix$animatedSprites = !sprites.isEmpty() ? ImmutableList.copyOf(sprites) : ImmutableList.of();
    }
}
