package org.embeddedt.vintagefix.mixin.bugfix.render_state_leaks;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.layers.LayerArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LayerArrow.class)
public abstract class LayerArrowMixin {
    /**
     * Use regular lighting enable/disable instead of the item one. It is unclear why Mojang used the item one.
     */
    @Redirect(
        method = "doRenderLayer",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/RenderHelper;disableStandardItemLighting()V"))
    private void disableLighting() {
        GlStateManager.disableLighting();
    }

    /**
     * Use regular lighting enable/disable instead of the item one. It is unclear why Mojang used the item one.
     */
    @Redirect(
        method = "doRenderLayer",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/RenderHelper;enableStandardItemLighting()V"
        )
    )
    private void enableLighting() {
        GlStateManager.enableLighting();
    }
}
