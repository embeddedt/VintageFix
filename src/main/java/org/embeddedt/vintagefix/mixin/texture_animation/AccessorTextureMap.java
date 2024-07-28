package org.embeddedt.vintagefix.mixin.texture_animation;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(TextureMap.class)
public interface AccessorTextureMap {
    @Accessor("mapUploadedSprites")
    Map<String, TextureAtlasSprite> vfix$getUploadedSprites();
}
