package org.embeddedt.vintagefix.mixin.bugfix.dark_entities;

import net.minecraft.client.renderer.entity.RenderMinecart;
import net.minecraft.entity.item.EntityMinecart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(RenderMinecart.class)
public class RenderMinecartMixin {
    @Redirect(
        method = "renderCartContents",
        at = @At(
            value = "INVOKE",
            /* Ignore the following error in IntelliJ, it does not understand the generics */
            target = "Lnet/minecraft/entity/item/EntityMinecart;getBrightness()F"
        )
    )
    private float getRealBrightness(EntityMinecart entity) {
        return 1.0f;
    }
}
