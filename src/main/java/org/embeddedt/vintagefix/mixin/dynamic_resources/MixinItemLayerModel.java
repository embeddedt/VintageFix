package org.embeddedt.vintagefix.mixin.dynamic_resources;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.client.model.ItemLayerModel;
import net.minecraftforge.common.model.TRSRTransformation;
import org.embeddedt.vintagefix.annotation.ClientOnlyMixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.vecmath.Vector4f;
import java.util.Optional;

@Mixin(ItemLayerModel.class)
@ClientOnlyMixin
public class MixinItemLayerModel {
    private static final Vector4f bakePosition = new Vector4f();

    @Inject(method = "buildQuad", at = @At("HEAD"), cancellable = true, remap = false)
    private static void buildQuadPacked(
        VertexFormat format, Optional<TRSRTransformation> transform, EnumFacing side, TextureAtlasSprite sprite, int tint,
        float x0, float y0, float z0, float u0, float v0,
        float x1, float y1, float z1, float u1, float v1,
        float x2, float y2, float z2, float u2, float v2,
        float x3, float y3, float z3, float u3, float v3,
        CallbackInfoReturnable<BakedQuad> cir)
    {
        if(format != DefaultVertexFormats.ITEM) {
            return;
        }

        TRSRTransformation tr;
        if(transform.isPresent() && !transform.get().isIdentity())
            tr = transform.get();
        else
            tr = null;

        int[] vertexData = new int[28];
        pumpVertex(vertexData, 0, x0, y0, z0, u0, v0, tr);
        pumpVertex(vertexData, 1, x1, y1, z1, u1, v1, tr);
        pumpVertex(vertexData, 2, x2, y2, z2, u2, v2, tr);
        pumpVertex(vertexData, 3, x3, y3, z3, u3, v3, tr);

        net.minecraftforge.client.ForgeHooksClient.fillNormal(vertexData, side);
        cir.setReturnValue(new BakedQuad(vertexData, tint, side, sprite, true, net.minecraft.client.renderer.vertex.DefaultVertexFormats.ITEM));
    }

    private static void pumpVertex(int[] data, int off, float x, float y, float z, float u, float v, TRSRTransformation transform) {
        off = off * 7;
        if(transform != null) {
            synchronized (bakePosition) {
                bakePosition.set(x, y, z, 1f);
                transform.transformPosition(bakePosition);
                x = bakePosition.x;
                y = bakePosition.y;
                z = bakePosition.z;
            }
        }
        data[off++] = Float.floatToRawIntBits(x);
        data[off++] = Float.floatToRawIntBits(y);
        data[off++] = Float.floatToRawIntBits(z);
        data[off++] = -1;
        data[off++] = Float.floatToRawIntBits(u);
        data[off++] = Float.floatToRawIntBits(v);
    }
}
