package org.embeddedt.vintagefix.mixin.texture_animation;

import com.google.common.collect.ImmutableList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
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
    @Shadow
    private VertexFormat vertexFormat;

    private final float[] vfix$texCoords = new float[8];
    private final ObjectOpenHashSet<TextureAtlasSprite> vfix$seenAnimatedSprites = new ObjectOpenHashSet<>();
    private SpriteFinderImpl vfix$spriteFinder;

    @Inject(method = "tex", at = @At("HEAD"))
    private void captureTex(double u, double v, CallbackInfoReturnable<BufferBuilder> cir) {
        if(drawMode != GL11.GL_QUADS) {
            return;
        }
        int vertex = this.vertexCount & 3;
        storeTexCoords((float)u, (float)v, vertex);
    }

    private void storeTexCoords(float u, float v, int vertex) {
        float[] list = vfix$texCoords;
        int i = vertex << 1;
        list[i] = u;
        list[i + 1] = v;
    }

    @Inject(method = "endVertex", at = @At("RETURN"))
    private void captureQuad(CallbackInfo ci) {
        if(drawMode == GL11.GL_QUADS && (this.vertexCount & 3) == 0) {
            captureAnimatedTexture();
        }
    }

    @Inject(method = "getVertexState", at = @At("RETURN"))
    private void copyAnimatedTextures(CallbackInfoReturnable<BufferBuilder.State> cir) {
        ((ExtendedBufferBuilderState)cir.getReturnValue()).vfix$setAnimatedSprites(this.vfix$seenAnimatedSprites.isEmpty() ? ImmutableList.of() : ImmutableList.copyOf(this.vfix$seenAnimatedSprites));
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

    @Inject(method = "addVertexData", at = @At("RETURN"))
    private void injectQuadSprites(int[] vertexData, CallbackInfo ci) {
        int vertexIntSize = this.vertexFormat.getIntegerSize();
        int numVertices = vertexData.length / vertexIntSize;
        int uvOffsetIdx = this.vertexFormat.getUvOffsetById(0) / 4;
        for(int i = 0; i < numVertices; i++) {
            float u = Float.intBitsToFloat(vertexData[uvOffsetIdx]);
            float v = Float.intBitsToFloat(vertexData[uvOffsetIdx + 1]);
            storeTexCoords(u, v, i);
            // After storing the fourth UV, capture a texture
            if((i & 3) == 3) {
                captureAnimatedTexture();
            }
            uvOffsetIdx += vertexIntSize;
        }
    }

    private void captureAnimatedTexture() {
        SpriteFinderImpl finder = vfix$spriteFinder;
        if(finder == null) {
            vfix$spriteFinder = finder = SpriteFinderImpl.get(Minecraft.getMinecraft().getTextureMapBlocks());
            if(finder == null) {
                return;
            }
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
