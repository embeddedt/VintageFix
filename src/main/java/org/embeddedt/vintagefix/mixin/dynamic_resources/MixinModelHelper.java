package org.embeddedt.vintagefix.mixin.dynamic_resources;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.embeddedt.vintagefix.annotation.ClientOnlyMixin;
import org.embeddedt.vintagefix.annotation.LateMixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import slimeknights.mantle.client.ModelHelper;

@Mixin(ModelHelper.class)
@ClientOnlyMixin
@LateMixin
public class MixinModelHelper {
    private static final int VERTEX_SIZE = DefaultVertexFormats.ITEM.getIntegerSize();

    /**
     * @author embeddedt
     * @reason avoid going through very slow UnpackedBakedQuad.Builder when we know format
     */
    @Inject(method = "colorQuad", at = @At("HEAD"), cancellable = true, remap = false)
    private static void colorQuadFast(int color, BakedQuad quad, CallbackInfoReturnable<BakedQuad> cir) {
        if(quad.getFormat() == DefaultVertexFormats.ITEM) {
            // If a is 0, make it 255 (matches ColorTransformer)
            if ((color & (0xff000000)) == 0) {
                color |= 0xff000000;
            }
            // ARGB -> ABGR
            color = Integer.reverseBytes(Integer.rotateLeft(color, 8));
            int[] newData = quad.getVertexData().clone();
            for(int i = 0; i < 4; i++) {
                newData[i * VERTEX_SIZE + 3] = color;
            }
            cir.setReturnValue(new BakedQuad(newData, quad.getTintIndex(), quad.getFace(), quad.getSprite(), quad.shouldApplyDiffuseLighting(), quad.getFormat()));
        }
    }
}
