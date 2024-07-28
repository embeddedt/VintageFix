package org.embeddedt.vintagefix.render;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;

import java.util.Collection;

public interface ExtendedBufferBuilderState {
    Collection<TextureAtlasSprite> vfix$getAnimatedSprites();
    void vfix$setAnimatedSprites(Collection<TextureAtlasSprite> sprites);
}
