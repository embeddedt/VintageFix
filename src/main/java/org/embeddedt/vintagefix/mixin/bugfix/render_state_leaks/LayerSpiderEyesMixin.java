package org.embeddedt.vintagefix.mixin.bugfix.render_state_leaks;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.layers.LayerSpiderEyes;
import net.minecraft.entity.monster.EntitySpider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LayerSpiderEyes.class)
public abstract class LayerSpiderEyesMixin {

    @Inject(method = "doRenderLayer", at = @At("RETURN"))
    private void reEnableDepthMask(EntitySpider spider, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, float scale, CallbackInfo ci) {
        GlStateManager.depthMask(true);
    }
}
