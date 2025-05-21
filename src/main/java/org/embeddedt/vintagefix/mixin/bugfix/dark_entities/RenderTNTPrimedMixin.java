package org.embeddedt.vintagefix.mixin.bugfix.dark_entities;

import net.minecraft.client.renderer.entity.RenderTNTPrimed;
import net.minecraft.entity.item.EntityTNTPrimed;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(RenderTNTPrimed.class)
public class RenderTNTPrimedMixin {
    @Redirect(
        method = "doRender",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/entity/item/EntityTNTPrimed;getBrightness()F"
        )
    )
    private float getRealBrightness(EntityTNTPrimed tnt) {
        return 1.0f;
    }
}
