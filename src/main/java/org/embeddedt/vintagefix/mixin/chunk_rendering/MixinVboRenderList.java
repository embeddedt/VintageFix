package org.embeddedt.vintagefix.mixin.chunk_rendering;

import net.minecraft.client.renderer.ChunkRenderContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.VboRenderList;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.client.renderer.vertex.VertexBuffer;
import net.minecraft.util.BlockRenderLayer;
import org.embeddedt.vintagefix.annotation.ClientOnlyMixin;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

@Mixin(VboRenderList.class)
@ClientOnlyMixin
public abstract class MixinVboRenderList extends ChunkRenderContainer {
    @Shadow
    abstract void setupArrayPointers();

    /**
     * @author embeddedt
     * @reason improve efficiency
     */
    @Overwrite
    public void renderChunkLayer(BlockRenderLayer layer)
    {
        if (this.initialized) {
            vfix$renderChunks(layer, this.renderChunks);
            OpenGlHelper.glBindBuffer(OpenGlHelper.GL_ARRAY_BUFFER, 0);
            GlStateManager.resetColor();
        }
    }

    private void vfix$renderChunks(BlockRenderLayer layer, List<RenderChunk> chunks) {
        //noinspection ForLoopReplaceableByForEach
        for(int i = 0; i < chunks.size(); i++) {
            vfix$renderChunk(layer, chunks.get(i));
        }
        chunks.clear();
    }

    private void vfix$renderChunk(BlockRenderLayer layer, RenderChunk chunk) {
        VertexBuffer vertexbuffer = chunk.getVertexBufferByLayer(layer.ordinal());
        GlStateManager.pushMatrix();
        this.preRenderChunk(chunk);
        chunk.multModelviewMatrix();
        vertexbuffer.bindBuffer();
        this.setupArrayPointers();
        vertexbuffer.drawArrays(GL11.GL_QUADS);
        GlStateManager.popMatrix();
    }
}
