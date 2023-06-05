package org.embeddedt.vintagefix.mixin.backports.new_world_name;

import net.minecraft.client.gui.GuiCreateWorld;
import net.minecraft.world.storage.ISaveFormat;
import org.embeddedt.vintagefix.annotation.ClientOnlyMixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GuiCreateWorld.class)
@ClientOnlyMixin
public class GuiCreateWorldMixin {
    @Inject(method = "getUncollidingSaveDirName", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/storage/ISaveFormat;getWorldInfo(Ljava/lang/String;)Lnet/minecraft/world/storage/WorldInfo;"), cancellable = true)
    private static void useNumberForCopies(ISaveFormat format, String worldName, CallbackInfoReturnable<String> cir) {
        int index = 1;
        String finalWorldName = worldName;
        while (format.getWorldInfo(finalWorldName) != null)
        {
            finalWorldName = worldName + " (" + index + ")";
            index++;
        }
        cir.setReturnValue(finalWorldName);
    }
}
