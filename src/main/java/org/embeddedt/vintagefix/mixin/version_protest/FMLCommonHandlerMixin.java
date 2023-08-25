package org.embeddedt.vintagefix.mixin.version_protest;

import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Loader;
import org.embeddedt.vintagefix.util.VersionProtester;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = FMLCommonHandler.class, remap = false)
public class FMLCommonHandlerMixin {
    @Redirect(method = "computeBranding", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/fml/common/Loader;getMCVersionString()Ljava/lang/String;"))
    private String addBranding(Loader instance) {
        return VersionProtester.protest(instance.getMCVersionString());
    }
}
