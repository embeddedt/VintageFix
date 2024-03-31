package org.embeddedt.vintagefix.mixin.dynamic_resources;

import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormatElement;
import net.minecraftforge.client.model.pipeline.LightUtil;
import org.embeddedt.vintagefix.annotation.ClientOnlyMixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(LightUtil.class)
@ClientOnlyMixin
public class MixinLightUtil {
    @Inject(method = "unpack", at = @At("RETURN"), locals = LocalCapture.CAPTURE_FAILHARD, remap = false)
    private static void unpackPositionW(int[] from, float[] to, VertexFormat formatFrom, int v, int e, CallbackInfo ci, int length, VertexFormatElement fromElement) {
        if(length >= 4 && fromElement.getElementCount() == 3 && fromElement.isPositionElement()) {
            // Forge unpacked baked quads store 1f in the last element, however the unpack method keeps the last component
            // as 0. Fix this discrepancy.
            to[3] = 1f;
        }
    }
}
