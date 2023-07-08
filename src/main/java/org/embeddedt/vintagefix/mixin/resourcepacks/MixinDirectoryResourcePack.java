package org.embeddedt.vintagefix.mixin.resourcepacks;

import com.teamacronymcoders.base.util.files.DirectoryResourcePack;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import org.embeddedt.vintagefix.annotation.ClientOnlyMixin;
import org.embeddedt.vintagefix.annotation.LateMixin;
import org.embeddedt.vintagefix.util.FolderPackCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.*;

@Mixin(DirectoryResourcePack.class)
@LateMixin
@ClientOnlyMixin
public class MixinDirectoryResourcePack {
    private FolderPackCache vfix_cache;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void afterConstruct(CallbackInfo ci) {
        vfix_cache = new FolderPackCache(new File(Minecraft.getMinecraft().gameDir, "resources"));
    }

    /*
    @Inject(method = "hasResourceName", at = @At("HEAD"), cancellable = true)
    private void useFastCheck(String name, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(vfix_cache.hasPath(name.replace("assets/", "")));
    }

     */
}
