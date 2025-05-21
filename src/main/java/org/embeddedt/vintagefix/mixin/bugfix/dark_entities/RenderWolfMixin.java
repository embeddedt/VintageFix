package org.embeddedt.vintagefix.mixin.bugfix.dark_entities;

import net.minecraft.client.renderer.entity.RenderWolf;
import net.minecraft.entity.passive.EntityWolf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(RenderWolf.class)
public class RenderWolfMixin {
    @Redirect(
        method = "doRender",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/passive/EntityWolf;getBrightness()F"
        )
    )
    private float getRealBrightness(EntityWolf entityWolf) {
        /*
         * The vanilla shading effect does not seem to be perceptible if the entity is rendered at full brightness
         * (1.0f), even though that is technically the correct value.
         * For that reason, the wolf is rendered at 75% brightness when in water instead. This mimics vanilla behavior
         * without the wolf turning completely black (MC-41825, MC-105248).
         */
        return 0.75f;
    }
}
