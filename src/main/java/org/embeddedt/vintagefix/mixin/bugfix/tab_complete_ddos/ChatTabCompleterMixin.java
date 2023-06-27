package org.embeddedt.vintagefix.mixin.bugfix.tab_complete_ddos;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.TabCompleter;
import net.minecraft.util.text.TextComponentString;
import org.embeddedt.vintagefix.annotation.ClientOnlyMixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Iterator;
import java.util.List;

@Mixin(GuiChat.ChatTabCompleter.class)
@ClientOnlyMixin
public abstract class ChatTabCompleterMixin extends TabCompleter {
    @Shadow
    @Final
    private Minecraft client;

    public ChatTabCompleterMixin(GuiTextField textFieldIn, boolean hasTargetBlockIn) {
        super(textFieldIn, hasTargetBlockIn);
    }

    private static final int MAX_COMPLETIONS_SHOWN = 100;

    @Redirect(method = "complete", at = @At(value = "INVOKE", target = "Ljava/util/List;iterator()Ljava/util/Iterator;"))
    private Iterator<String> getSubCompletions(List<String> completions) {
        return completions.subList(0, Math.min(MAX_COMPLETIONS_SHOWN, completions.size())).iterator();
    }

    @Inject(method = "complete", at = @At("RETURN"))
    private void printAbridgedMessage(CallbackInfo ci) {
        if(this.completions.size() > MAX_COMPLETIONS_SHOWN) {
            this.client.ingameGUI.getChatGUI().printChatMessageWithOptionalDeletion(new TextComponentString("(only first " + MAX_COMPLETIONS_SHOWN + " shown)"), 2);
        }
    }
}
