package org.embeddedt.vintagefix.mixin.bugfix.render_state_leaks;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderGuardian;
import net.minecraft.entity.monster.EntityGuardian;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderGuardian.class)
public abstract class RenderGuardianMixin {

    @Inject(method = "doRender", at = @At("RETURN"))
    public void resetGlState(EntityGuardian entity, double x, double y, double z, float entityYaw, float partialTicks, CallbackInfo ci) {
        if(entity.getTargetedEntity() != null) {
            GlStateManager.enableLighting();
            GlStateManager.enableCull();
        }
    }
}
