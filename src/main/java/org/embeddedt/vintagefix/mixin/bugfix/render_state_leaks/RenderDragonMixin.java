package org.embeddedt.vintagefix.mixin.bugfix.render_state_leaks;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderDragon;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(RenderDragon.class)
public abstract class RenderDragonMixin {
    /**
     * Use regular lighting enable/disable instead of the item one. It is unclear why Mojang used the item one.
     */
    @Redirect(
        method = "renderCrystalBeams",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/RenderHelper;disableStandardItemLighting()V"
        )
    )
    private static void disableLighting() {
        GlStateManager.disableLighting();
    }

    /**
     * Use regular lighting enable/disable instead of the item one. It is unclear why Mojang used the item one.
     */
    @Redirect(
        method = "renderCrystalBeams",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/RenderHelper;enableStandardItemLighting()V"
        )
    )
    private static void enableLighting() {
        GlStateManager.enableLighting();
    }
}
