package org.embeddedt.vintagefix.mixin.dynamic_resources;

import net.minecraft.client.renderer.RenderItem;
import org.embeddedt.vintagefix.annotation.ClientOnlyMixin;
import org.embeddedt.vintagefix.dynamicresources.ItemBakeThread;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderItem.class)
@ClientOnlyMixin
public class MixinRenderItem {
    @Inject(method = "onResourceManagerReload", at = @At("HEAD"))
    private void doBake(CallbackInfo ci) {
        ItemBakeThread.restartBake();
    }
}
