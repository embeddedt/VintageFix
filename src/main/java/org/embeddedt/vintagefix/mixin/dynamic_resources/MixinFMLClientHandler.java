package org.embeddedt.vintagefix.mixin.dynamic_resources;

import net.minecraftforge.fml.client.FMLClientHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FMLClientHandler.class)
public class MixinFMLClientHandler {
    /**
     * Due to the aggressive texture scan, this logging is pretty much useless as a lot of files can be picked up
     * that don't exist or aren't valid textures.
     */
    @Inject(method = "logMissingTextureErrors", at = @At("HEAD"), cancellable = true, remap = false)
    private void silenceMissingTextures(CallbackInfo ci) {
        ci.cancel();
    }
}
