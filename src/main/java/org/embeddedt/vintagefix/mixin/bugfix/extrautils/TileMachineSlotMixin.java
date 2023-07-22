package org.embeddedt.vintagefix.mixin.bugfix.extrautils;

import net.minecraft.item.ItemStack;
import org.embeddedt.vintagefix.annotation.LateMixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = { "com/rwtema/extrautils2/machine/TileMachine$7", "com/rwtema/extrautils2/machine/TileMachine$8"})
@LateMixin
public class TileMachineSlotMixin {
    @Redirect(method = "getStack", at = @At(value = "INVOKE", target = "Lcom/rwtema/extrautils2/compatibility/StackHelper;safeCopy(Lnet/minecraft/item/ItemStack;)Lnet/minecraft/item/ItemStack;"))
    private ItemStack skipCopy(ItemStack original) {
        return original;
    }
}
