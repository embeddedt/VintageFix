package org.embeddedt.vintagefix.mixin.backports.white_button_text;

import net.minecraft.client.gui.GuiButton;
import org.embeddedt.vintagefix.VintageFixClient;
import org.embeddedt.vintagefix.annotation.ClientOnlyMixin;
import org.embeddedt.vintagefix.annotation.LateMixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Pseudo
@Mixin(targets = {"mezz/jei/gui/elements/GuiIconButton", "mezz/jei/gui/elements/GuiIconButtonSmall"})
@ClientOnlyMixin
@LateMixin
public abstract class MixinJEIGuiIconButton extends GuiButton {
    public MixinJEIGuiIconButton(int buttonId, int x, int y, String buttonText) {
        super(buttonId, x, y, buttonText);
    }

    @ModifyConstant(method = "func_191745_a", constant = @Constant(intValue = 16777120), require = 0)
    private int ignoreHoverIfModernity(int oldColor) {
        return VintageFixClient.modernityPresent ? 14737632 : oldColor;
    }
}
