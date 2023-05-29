package org.embeddedt.vintagefix.core;

import com.google.common.collect.ImmutableMap;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.embeddedt.vintagefix.transformercache.TransformerCache;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MixinConfigPlugin implements IMixinConfigPlugin {
    private static final Logger LOGGER = LogManager.getLogger("VintageFix Mixin Loader");

    private static final ImmutableMap<String, Consumer<PotentialMixin>> MIXIN_PROCESSING_MAP = ImmutableMap.<String, Consumer<PotentialMixin>>builder()
        .put("Lorg/spongepowered/asm/mixin/Mixin;", p -> p.valid = true)
        .put("Lorg/embeddedt/vintagefix/annotation/ClientOnlyMixin;", p -> p.isClientOnly = true)
        .put("Lorg/embeddedt/vintagefix/annotation/LateMixin;", p -> p.isLate = true)
        .build();

    static class PotentialMixin {
        String className;
        boolean valid;
        boolean isClientOnly;
        boolean isLate;
    }

    private final List<PotentialMixin> allMixins = new ArrayList<>();

    private void considerClass(String pathString) throws IOException {
        try(InputStream stream = MixinConfigPlugin.class.getClassLoader().getResourceAsStream("org/embeddedt/vintagefix/mixin/" + pathString)) {
            if(stream == null)
                return;
            ClassReader reader = new ClassReader(stream);
            ClassNode node = new ClassNode();
            reader.accept(node,  ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
            if(node.invisibleAnnotations == null)
                return;
            PotentialMixin mixin = new PotentialMixin();
            mixin.className = node.name.replace('/', '.');
            for(AnnotationNode annotation : node.invisibleAnnotations) {
                Consumer<PotentialMixin> consumer = MIXIN_PROCESSING_MAP.get(annotation.desc);
                if(consumer != null)
                    consumer.accept(mixin);
            }
            if(mixin.valid && LateMixins.atLateStage == mixin.isLate)
                allMixins.add(mixin);
        }
    }
    @Override
    public void onLoad(String s) {
        try {
            URI uri = Objects.requireNonNull(MixinConfigPlugin.class.getResource("/mixins.vintagefix.json")).toURI();
            FileSystem fs;
            try {
                fs = FileSystems.getFileSystem(uri);
            } catch (FileSystemNotFoundException var11) {
                fs = FileSystems.newFileSystem(uri, Collections.emptyMap());
            }
            List<Path> list;
            Path basePath = fs.getPath("org", "embeddedt", "vintagefix", "mixin").toAbsolutePath();
            try(Stream<Path> stream = Files.walk(basePath)) {
                list = stream.collect(Collectors.toList());
            }
            for(Path p : list) {
                if(p == null)
                    continue;
                p = basePath.relativize(p.toAbsolutePath());
                String pathString = p.toString();
                if(pathString.endsWith(".class")) {
                    considerClass(pathString);
                }
            }
        } catch(IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
        LOGGER.info("Found {} {} mixins", allMixins.size(), LateMixins.atLateStage ? "late" : "early");
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String s, String s1) {
        return true;
    }

    @Override
    public void acceptTargets(Set<String> set, Set<String> set1) {
    }

    @Override
    public List<String> getMixins() {
        MixinEnvironment.Phase phase = MixinEnvironment.getCurrentEnvironment().getPhase();
        if(phase == MixinEnvironment.Phase.DEFAULT) {
            if(!LateMixins.atLateStage && Boolean.getBoolean("vintagefix.transformerCache")) {
                TransformerCache.instance.init();
            }
            MixinEnvironment.Side side = MixinEnvironment.getCurrentEnvironment().getSide();
            List<String> list = allMixins.stream()
                .filter(p -> !p.isClientOnly || side == MixinEnvironment.Side.CLIENT)
                .map(p -> p.className)
                .map(clz -> clz.replace("org.embeddedt.vintagefix.mixin.", ""))
                .collect(Collectors.toList());
            for(String mixin : list) {
                LOGGER.debug("loading {}", mixin);
            }
            return list;
        }
        return null;
    }

    @Override
    public void preApply(String s, ClassNode classNode, String s1, IMixinInfo iMixinInfo) {

    }

    @Override
    public void postApply(String s, ClassNode classNode, String s1, IMixinInfo iMixinInfo) {

    }
}
