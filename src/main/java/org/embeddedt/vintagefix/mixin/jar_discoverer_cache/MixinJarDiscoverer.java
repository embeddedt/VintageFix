package org.embeddedt.vintagefix.mixin.jar_discoverer_cache;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.ModContainerFactory;
import net.minecraftforge.fml.common.discovery.ASMDataTable;
import net.minecraftforge.fml.common.discovery.JarDiscoverer;
import net.minecraftforge.fml.common.discovery.ModCandidate;
import net.minecraftforge.fml.common.discovery.asm.ASMModParser;
import org.embeddedt.vintagefix.jarcache.JarDiscovererCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.network.play.INetHandlerPlayClient;

import static org.embeddedt.vintagefix.VintageFix.LOGGER;

// TODO figure out why we implement INetHandlerPlayClient...
@Mixin(value = JarDiscoverer.class, remap = false)
public abstract class MixinJarDiscoverer implements INetHandlerPlayClient {

    private ZipEntry lastZipEntry;

    String lastHash;
    JarDiscovererCache.CachedModInfo lastCMI;

    /** Load the saved result if the jar's path and modification date haven't changed. */
    @Inject(method = "discover", at = @At("HEAD"))
    public void preDiscover(ModCandidate candidate, ASMDataTable table, CallbackInfoReturnable cir) {
        String hash = null;
        File file = candidate.getModContainer();
        hash = file.getPath() + "@" + file.lastModified();

        lastHash = hash;
        lastCMI = JarDiscovererCache.getCachedModInfo(lastHash);

        LOGGER.debug("preDiscover " + candidate.getModContainer() + "(hash " + lastHash + ")");
    }

    /** Store ZipEntry reference for later. */
    @Redirect(method = "findClassesASM", at = @At(value = "INVOKE", target = "Ljava/util/jar/JarFile;getInputStream(Ljava/util/zip/ZipEntry;)Ljava/io/InputStream;"))
    public InputStream redirectGetInputStream(JarFile jf, ZipEntry ze) throws IOException {
        lastZipEntry = ze;
        return jf.getInputStream(ze);
    }

    /** Try to load cached ASMModParser instead of creating a new one. */
    @Redirect(method = "findClassesASM", at = @At(value = "NEW", target = "(Ljava/io/InputStream;)Lnet/minecraftforge/fml/common/discovery/asm/ASMModParser;"))
    public ASMModParser redirectNewASMModParser(InputStream stream, ModCandidate candidate, ASMDataTable table) throws IOException {
        ASMModParser parser = lastCMI.getCachedParser(lastZipEntry);
        if(parser == null) {
            try {
                parser = new ASMModParser(stream);
            } finally {
                stream.close();
            }
            lastCMI.putParser(lastZipEntry, parser);
        }
        return parser;
    }

    /** Remember if the ModContainer was null last time; if it was, return null instead of trying to create one. */
    @Redirect(method = "findClassesASM", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/fml/common/ModContainerFactory;build(Lnet/minecraftforge/fml/common/discovery/asm/ASMModParser;Ljava/io/File;Lnet/minecraftforge/fml/common/discovery/ModCandidate;)Lnet/minecraftforge/fml/common/ModContainer;"))
    public ModContainer redirectBuild(ModContainerFactory factory, ASMModParser modParser, File modSource, ModCandidate container, ModCandidate candidate, ASMDataTable table) {
        int isModClass = lastCMI.getCachedIsModClass(lastZipEntry);
        ModContainer mc = null;
        if(isModClass != 0) {
            mc = factory.build(modParser, modSource, container);
            if(isModClass == -1) {
                lastCMI.putIsModClass(lastZipEntry, mc != null);
            }
        }
        return mc;
    }
}
