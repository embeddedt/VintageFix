package org.embeddedt.vintagefix.mixin.backports.white_button_text;

import net.minecraft.client.gui.GuiButton;
import org.embeddedt.vintagefix.VintageFixClient;
import org.embeddedt.vintagefix.annotation.ClientOnlyMixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(GuiButton.class)
@ClientOnlyMixin
public class MixinGuiButton {
    @ModifyConstant(method = "drawButton", constant = @Constant(intValue = 16777120))
    private int ignoreHoverIfModernity(int oldColor) {
        return VintageFixClient.modernityPresent ? 14737632 : oldColor;
    }
}
