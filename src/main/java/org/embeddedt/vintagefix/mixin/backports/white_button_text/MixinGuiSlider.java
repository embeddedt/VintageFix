package org.embeddedt.vintagefix.mixin.backports.white_button_text;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiOptionSlider;
import net.minecraft.client.gui.GuiSlider;
import org.embeddedt.vintagefix.VintageFixClient;
import org.embeddedt.vintagefix.annotation.ClientOnlyMixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(value = {GuiSlider.class, GuiOptionSlider.class})
@ClientOnlyMixin
public abstract class MixinGuiSlider extends GuiButton {
    public MixinGuiSlider(int buttonId, int x, int y, String buttonText) {
        super(buttonId, x, y, buttonText);
    }

    /**
     * Use the hover variant of the button texture.
     */
    @ModifyConstant(method = "mouseDragged", constant = @Constant(intValue = 66))
    protected int getTextureY(int y) {
        return (VintageFixClient.modernityPresent && this.hovered) ? (y + 20) : y;
    }
}
