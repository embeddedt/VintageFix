package org.embeddedt.vintagefix.mixin.resourcepacks;

import lumien.resourceloader.loader.NormalResourceLoader;
import lumien.resourceloader.loader.OverridingResourceLoader;
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

@Mixin({ NormalResourceLoader.class, OverridingResourceLoader.class })
@LateMixin
@ClientOnlyMixin
public class MixinNormalResourceLoader {
    private FolderPackCache vfix_cache;

    private String ourFolderName;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void afterConstruct(CallbackInfo ci) {
        ourFolderName = ((Object)this instanceof OverridingResourceLoader) ? "oresources" : "resources";
        vfix_cache = new FolderPackCache(new File(Minecraft.getMinecraft().gameDir, ourFolderName));
    }

    /*

    @Inject(method = "resourceExists", at = @At("HEAD"), cancellable = true)
    private void useFastCheck(ResourceLocation rl, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(vfix_cache.hasPath(rl.getNamespace() + "/" + rl.getPath()));
    }

    @Inject(method = "getInputStream", at = @At("HEAD"), cancellable = true)
    private void getFastInputStream(ResourceLocation rl, CallbackInfoReturnable<InputStream> cir) throws IOException {
        try {
            cir.setReturnValue(new FileInputStream(new File(new File(Minecraft.getMinecraft().gameDir, ourFolderName + "/" + rl.getNamespace()), rl.getPath())));
        } catch(FileNotFoundException e) {
            cir.setReturnValue(null);
        }
    }

     */
}
