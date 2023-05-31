package org.embeddedt.vintagefix.mixin.resourcepacks;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableSet;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.client.resources.FileResourcePack;
import org.embeddedt.vintagefix.VintageFix;
import org.embeddedt.vintagefix.annotation.ClientOnlyMixin;
import org.embeddedt.vintagefix.util.CachedResourcePath;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Mixin(FileResourcePack.class)
@ClientOnlyMixin
public abstract class MixinFileResourcePack {
    @Shadow
    protected abstract ZipFile getResourcePackZipFile() throws IOException;

    private static final Cache<String, ObjectOpenHashSet<CachedResourcePath>> containedPathsByFile = CacheBuilder.newBuilder()
        .concurrencyLevel(4)
        .expireAfterAccess(5, TimeUnit.MINUTES)
        .build();

    private Set<CachedResourcePath> genCache() {
        ZipFile file;
        try {
            file = this.getResourcePackZipFile();
            return containedPathsByFile.get(file.getName(), () -> {
                ObjectOpenHashSet<CachedResourcePath> containedPaths = new ObjectOpenHashSet<>(file.size());
                Enumeration<? extends ZipEntry> entryEnum = file.entries();
                while(entryEnum.hasMoreElements()) {
                    ZipEntry entry = entryEnum.nextElement();
                    if(entry.getName().startsWith("assets/") || entry.getName().indexOf('/') == -1)
                        containedPaths.add(new CachedResourcePath(entry.getName(), true));
                }
                containedPaths.trim();
                return containedPaths;
            });
        } catch(IOException | ExecutionException e) {
            VintageFix.LOGGER.error("Exception creating cache", e);
            return ImmutableSet.of();
        }
    }

    @Inject(method = "hasResourceName", at = @At("HEAD"), cancellable = true)
    private void fastHasResource(String name, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(genCache().contains(new CachedResourcePath(name, false)));
    }
}
