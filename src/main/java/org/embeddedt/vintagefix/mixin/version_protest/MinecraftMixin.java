package org.embeddedt.vintagefix.mixin.version_protest;

import net.minecraft.client.Minecraft;
import org.embeddedt.vintagefix.util.VersionProtester;
import org.lwjgl.opengl.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Inject(method = "createDisplay", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/Display;setTitle(Ljava/lang/String;)V", shift = At.Shift.AFTER, remap = false), require = 0)
    private void protestTitle(CallbackInfo ci) {
        String old = Display.getTitle();
        String newS = VersionProtester.protest(old);
        if(!old.equals(newS))
            Display.setTitle(newS);
    }
}
