package org.embeddedt.vintagefix.mixin.texture_animation;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.embeddedt.vintagefix.annotation.ClientOnlyMixin;
import org.embeddedt.vintagefix.render.ExtendedBufferBuilderState;
import org.embeddedt.vintagefix.render.ExtendedTextureAtlasSprite;
import org.embeddedt.vintagefix.render.SpriteFinderImpl;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;

@Mixin(BufferBuilder.class)
@ClientOnlyMixin
public class MixinBufferBuilder implements ExtendedBufferBuilderState {
    @Shadow
    private int vertexCount, drawMode;

    private final float[] vfix$texCoords = new float[8];
    private final ObjectOpenHashSet<TextureAtlasSprite> vfix$seenAnimatedSprites = new ObjectOpenHashSet<>();
    private final SpriteFinderImpl vfix$spriteFinder = SpriteFinderImpl.get(Minecraft.getMinecraft().getTextureMapBlocks());

    @Inject(method = "tex", at = @At("HEAD"))
    private void captureTex(double u, double v, CallbackInfoReturnable<BufferBuilder> cir) {
        if(drawMode != GL11.GL_QUADS) {
            return;
        }
        float[] list = vfix$texCoords;
        int vertex = this.vertexCount & 3;
        int i = vertex << 1;
        list[i] = (float)u;
        list[i + 1] = (float)v;
    }

    @Inject(method = "endVertex", at = @At("RETURN"))
    private void captureQuad(CallbackInfo ci) {
        if(drawMode == GL11.GL_QUADS && (this.vertexCount & 3) == 0) {
            captureAnimatedTexture();
        }
    }

    @Inject(method = "setVertexState", at = @At("RETURN"))
    private void copyAnimatedTextures(BufferBuilder.State state, CallbackInfo ci) {
        this.vfix$seenAnimatedSprites.clear();
        Collection<TextureAtlasSprite> sprites = ((ExtendedBufferBuilderState)state).vfix$getAnimatedSprites();
        if(!sprites.isEmpty()) {
            this.vfix$seenAnimatedSprites.addAll(sprites);
        }
    }

    @Inject(method = "reset", at = @At("RETURN"))
    private void clearAnimatedTextures(CallbackInfo ci) {
        this.vfix$seenAnimatedSprites.clear();
    }

    private void captureAnimatedTexture() {
        SpriteFinderImpl finder = this.vfix$spriteFinder;
        if(finder == null) {
            return;
        }
        float[] uvs = this.vfix$texCoords;
        float centerU = 0, centerV = 0;
        for(int vertex = 0; vertex < 4; vertex++) {
            int idx = vertex * 2;
            centerU += uvs[idx];
            centerV += uvs[idx + 1];
        }
        centerU /= 4;
        centerV /= 4;
        TextureAtlasSprite sprite = finder.find(centerU, centerV);
        if (sprite != null && sprite.hasAnimationMetadata()) {
            // Activate the sprite immediately in case we don't catch it later
            ((ExtendedTextureAtlasSprite)sprite).vfix$setActive(true);
            this.vfix$seenAnimatedSprites.add(sprite);
        }
    }

    @Override
    public Collection<TextureAtlasSprite> vfix$getAnimatedSprites() {
        return this.vfix$seenAnimatedSprites;
    }

    @Override
    public void vfix$setAnimatedSprites(Collection<TextureAtlasSprite> sprites) {
        this.vfix$seenAnimatedSprites.clear();
        this.vfix$seenAnimatedSprites.addAll(sprites);
    }
}
