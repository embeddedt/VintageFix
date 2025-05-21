package org.embeddedt.vintagefix.mixin.bugfix.render_state_leaks;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.layers.LayerCreeperCharge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LayerCreeperCharge.class)
public abstract class LayerCreeperChargeMixin {

    @Redirect(
        method = "doRenderLayer",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/GlStateManager;depthMask(Z)V",
            ordinal = 1 /* target second depthMask only */
        )
    )
    private void alwaysReenableDepthMask(boolean flagIn) {
        GlStateManager.depthMask(true);
    }
}
