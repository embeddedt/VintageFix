package org.embeddedt.vintagefix.mixin.texture_animation;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.embeddedt.vintagefix.annotation.ClientOnlyMixin;
import org.embeddedt.vintagefix.render.ExtendedTextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(TextureAtlasSprite.class)
@ClientOnlyMixin
public class MixinTextureAtlasSprite implements ExtendedTextureAtlasSprite {
    private boolean vfix$isActiveThisFrame = false;

    @Override
    public boolean vfix$isActive() {
        return vfix$isActiveThisFrame;
    }

    @Override
    public void vfix$setActive(boolean active) {
        this.vfix$isActiveThisFrame = active;
    }
}
