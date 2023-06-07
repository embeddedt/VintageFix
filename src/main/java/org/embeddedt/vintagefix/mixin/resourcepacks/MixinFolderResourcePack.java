package org.embeddedt.vintagefix.mixin.resourcepacks;

import net.minecraft.client.resources.FolderResourcePack;
import org.embeddedt.vintagefix.annotation.ClientOnlyMixin;
import org.embeddedt.vintagefix.util.FolderPackCache;
import org.embeddedt.vintagefix.util.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;
import java.io.IOException;

@Mixin(FolderResourcePack.class)
@ClientOnlyMixin
public class MixinFolderResourcePack {
    private FolderPackCache vfix_cache;

    @Inject(method = "<init>*", at = @At("RETURN"))
    private void afterConstructor(File folder, CallbackInfo ci) {
        vfix_cache = new FolderPackCache(folder);
    }

    @Redirect(method = "validatePath", at = @At(value = "INVOKE", target = "Ljava/io/File;getCanonicalPath()Ljava/lang/String;"))
    private static String getCanonicalPathFast(File file) throws IOException {
        return Util.getCanonicalPathFast(file);
    }

    @Redirect(method = "getFile",
        at = @At(value = "INVOKE", target = "Ljava/io/File;isFile()Z", remap = false))
    public boolean redirectIsFile(File file, String name) {
        return vfix_cache.hasPath(name);
    }
}
