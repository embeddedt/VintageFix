package org.embeddedt.vintagefix.core;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.llamalad7.mixinextras.MixinExtrasBootstrap;
import net.minecraft.launchwrapper.Launch;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Loader;
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

import javax.annotation.Nullable;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MixinConfigPlugin implements IMixinConfigPlugin {
    private static final Logger LOGGER = LogManager.getLogger("VintageFix Mixin Loader");

    private static final String PACKAGE_PREFIX = "org.embeddedt.vintagefix.";

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

    private static final List<PotentialMixin> allMixins = new ArrayList<>();

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
            if(mixin.valid)
                allMixins.add(mixin);
        }
    }

    private static Properties config;

    private static String mixinClassNameToBaseName(String mixinClassName) {
        String noPrefix = mixinClassName.replace(PACKAGE_PREFIX, "");
        return noPrefix.substring(0, noPrefix.lastIndexOf('.'));
    }

    @SuppressWarnings("unchecked")
    private static void writeOrderedProperties(Properties props, OutputStream stream) throws IOException {
        try(PrintWriter writer = new PrintWriter(stream)) {
            writer.println("# VintageFix config file");
            writer.println();
            List<String> lst = new ArrayList<>((Set<String>)(Set<?>)props.keySet());
            lst.sort(Comparator.naturalOrder());
            for(String k : lst) {
                writer.println(k + "=" + props.getProperty(k));
            }
        }
    }

    private static final ImmutableMap<String, Boolean> extraBaseNames = ImmutableMap.<String, Boolean>builder()
        .put("mixin.dynamic_resources.background_item_bake", true)
        .put("mixin.bugfix.extrautils", false)
        .put("mixin.version_protest", false)
        .put("mixin.dynamic_resources.hide_model_exceptions", false)
        .build();

    @Override
    public void onLoad(String s) {
        if(allMixins.size() == 0) {
            // Fallback in case the other plugin doesn't work in dev
            MixinExtrasBootstrap.init();
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
            LOGGER.info("Found {} mixins", allMixins.size());
            config = new Properties();
            File targetConfig = new File(Launch.minecraftHome, "config" + File.separator + "vintagefix.properties");
            try {
                if(targetConfig.exists()) {
                    try(InputStream stream = Files.newInputStream(targetConfig.toPath())) {
                        config.load(stream);
                    } catch(IllegalArgumentException ignored) {}
                }
                for(PotentialMixin m : allMixins) {
                    String baseName = mixinClassNameToBaseName(m.className);
                    if(!config.containsKey(baseName)) {
                        String value = extraBaseNames.getOrDefault(baseName, true).toString();
                        LOGGER.warn("Added missing entry '{}' to config file with default value '{}'", baseName, value);
                        config.put(baseName, value);
                    }
                }
                for(Map.Entry<String, Boolean> entry : extraBaseNames.entrySet()) {
                    if(!config.containsKey(entry.getKey()))
                        config.put(entry.getKey(), entry.getValue().toString());
                }
                try(OutputStream stream = Files.newOutputStream(targetConfig.toPath())) {
                    writeOrderedProperties(config, stream);
                }
                LOGGER.info("Successfully saved config file");
            } catch(IOException e) {
                LOGGER.error("Exception handling config", e);
            }
        }
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetName, String className) {
        return true;
    }

    @Override
    public void acceptTargets(Set<String> set, Set<String> set1) {
    }

    private static final List<String> VINTAGIUM_DISABLED_PACKAGES = ImmutableList.of("mixin.chunk_rendering", "mixin.bugfix.entity_disappearing", "mixin.invisible_subchunks");
    private static final List<String> OPTIFINE_DISABLED_PACKAGES = ImmutableList.<String>builder().addAll(VINTAGIUM_DISABLED_PACKAGES).add("mixin.textures").add("mixin.bugfix.ao_artifacts").build();
    private static final List<String> SLEDGEHAMMER_DISABLED_PACKAGES = ImmutableList.<String>builder().add("mixin.bugfix.dark_entities").add("mixin.bugfix.render_state_leaks").build();

    public static boolean isMixinClassApplied(String name) {
        String unprefixedName = name.replace(PACKAGE_PREFIX, "");
        String baseName = mixinClassNameToBaseName(name);
        // texture optimization causes issues when OF is installed
        if(VintageFixCore.OPTIFINE && OPTIFINE_DISABLED_PACKAGES.stream().anyMatch(baseName::startsWith)) {
            return false;
        }
        if(VintageFixCore.VINTAGIUM && VINTAGIUM_DISABLED_PACKAGES.stream().anyMatch(baseName::startsWith)) {
            return false;
        }
        if(VintageFixCore.SLEDGEHAMMER && SLEDGEHAMMER_DISABLED_PACKAGES.stream().anyMatch(baseName::startsWith)) {
            return false;
        }
        // property optimizations are redundant with Sponge installed
        if(unprefixedName.startsWith("mixin.blockstates.Property") && VintageFixCore.SPONGE) {
            return false;
        }
        // check the config
        boolean isEnabled = Boolean.parseBoolean(config.getProperty(baseName, ""));
        if(!isEnabled) {
            LOGGER.warn("Not applying mixin '{}' as '{}' is disabled in config", name, baseName);
        }
        return isEnabled;
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
                .filter(p -> p.isLate == LateMixins.atLateStage)
                .map(p -> p.className)
                .filter(MixinConfigPlugin::isMixinClassApplied)
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
