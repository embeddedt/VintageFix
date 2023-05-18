package org.embeddedt.vintagefix.mixin.resourcepacks;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.client.resources.FileResourcePack;
import org.embeddedt.vintagefix.util.CachedResourcePath;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.IOException;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Mixin(FileResourcePack.class)
public abstract class MixinFileResourcePack {
    @Shadow
    protected abstract ZipFile getResourcePackZipFile() throws IOException;

    private final Object2ObjectOpenHashMap<CachedResourcePath, ZipEntry> containedPaths = new Object2ObjectOpenHashMap<>();

    @Inject(method = "<init>", at = @At("RETURN"))
    private void genCache(CallbackInfo ci) {
        ZipFile zf;
        try {
            zf = this.getResourcePackZipFile();
        } catch(IOException e) {
            return;
        }

        Enumeration<? extends ZipEntry> entryEnum = zf.entries();
        while(entryEnum.hasMoreElements()) {
            ZipEntry entry = entryEnum.nextElement();
            containedPaths.put(new CachedResourcePath(entry.getName(), true), entry);
        }
        containedPaths.trim();
    }

    @Inject(method = "hasResourceName", at = @At("HEAD"), cancellable = true)
    private void fastHasResource(String name, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(containedPaths.containsKey(new CachedResourcePath(name, false)));
    }

    @Redirect(method = "getInputStreamByName", at = @At(value = "INVOKE", target = "Ljava/util/zip/ZipFile;getEntry(Ljava/lang/String;)Ljava/util/zip/ZipEntry;"))
    private ZipEntry getZipEntryFast(ZipFile file, String name) {
        return containedPaths.get(new CachedResourcePath(name, false));
    }
}
