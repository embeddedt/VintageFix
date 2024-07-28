package org.embeddedt.vintagefix.mixin.texture_animation;

import com.google.common.collect.Iterators;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import org.embeddedt.vintagefix.annotation.ClientOnlyMixin;
import org.embeddedt.vintagefix.render.ExtendedTextureAtlasSprite;
import org.embeddedt.vintagefix.render.SpriteFinderImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Iterator;
import java.util.List;

@Mixin(TextureMap.class)
@ClientOnlyMixin
public class MixinTextureMap implements SpriteFinderImpl.SpriteFinderAccess {
    private SpriteFinderImpl vfix$spriteFinder;

    @Override
    public SpriteFinderImpl fabric_spriteFinder() {
        return vfix$spriteFinder;
    }

    @Override
    public void fabric_setSpriteFinder(SpriteFinderImpl impl) {
        vfix$spriteFinder = impl;
    }

    @Redirect(method = "updateAnimations", at = @At(value = "INVOKE", target = "Ljava/util/List;iterator()Ljava/util/Iterator;"))
    private Iterator<TextureAtlasSprite> getFilteredIterator(List<TextureAtlasSprite> sprites) {
        if((Object)this != Minecraft.getMinecraft().getTextureMapBlocks()) {
            return sprites.iterator();
        }
        return Iterators.filter(sprites.iterator(), sprite -> {
            ExtendedTextureAtlasSprite spriteExt = (ExtendedTextureAtlasSprite)sprite;
            if(spriteExt.vfix$isActive()) {
                spriteExt.vfix$setActive(false);
                return true;
            } else {
                return false;
            }
        });
    }

}
