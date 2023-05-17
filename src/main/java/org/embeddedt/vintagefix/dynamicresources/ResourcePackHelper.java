package org.embeddedt.vintagefix.dynamicresources;

import com.google.common.collect.ImmutableList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.client.resources.*;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import org.embeddedt.vintagefix.VintageFix;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ResourcePackHelper {
    private static final Method GET_FILE_PACK_ZIP_FILE = ObfuscationReflectionHelper.findMethod(FileResourcePack.class, "func_110599_c", ZipFile.class);

    private static FileSystem obtainFileSystem(Class<?> clz) throws IOException {
        try {
            URI uri = clz.getResource("/assets/.mcassetsroot").toURI();
            if ("jar".equals(uri.getScheme())) {
                try {
                    return FileSystems.getFileSystem(uri);
                } catch (FileSystemNotFoundException var11) {
                    return FileSystems.newFileSystem(uri, Collections.emptyMap());
                }
            } else
                throw new IOException("Wrong URI scheme: " + uri.getScheme());
        } catch (URISyntaxException e) {
            throw new IOException("Couldn't list vanilla resources", e);
        }
    }

    public static Collection<IResourcePack> getAllPacks(SimpleReloadableResourceManager manager) {
        Set<IResourcePack> resourcePacks = new LinkedHashSet<>();
        Map<String, FallbackResourceManager> domainManagers = ObfuscationReflectionHelper.getPrivateValue(SimpleReloadableResourceManager.class, manager, "field_110548_a");
        for (FallbackResourceManager fallback : domainManagers.values()) {
            List<IResourcePack> fallbackPacks = ObfuscationReflectionHelper.getPrivateValue(FallbackResourceManager.class, fallback, "field_110540_a");
            resourcePacks.addAll(fallbackPacks);
        }
        return resourcePacks;
    }

    public static Collection<String> getAllPaths(SimpleReloadableResourceManager manager, Predicate<String> filter) {
        Collection<IResourcePack> resourcePacks = getAllPacks(manager);
        Set<String> paths = new ObjectOpenHashSet<>();
        for (IResourcePack pack : resourcePacks) {
            try {
                paths.addAll(getAllPaths(pack, filter));
            } catch(IOException e) {
                e.printStackTrace();
            }
        }
        return paths;
    }

    public static Collection<String> getAllPaths(IResourcePack pack, Predicate<String> filter) throws IOException {
        if (pack instanceof LegacyV2Adapter) {
            IResourcePack wrappedPack = ObfuscationReflectionHelper.getPrivateValue(LegacyV2Adapter.class, (LegacyV2Adapter) pack, "field_191383_a");
            return getAllPaths(wrappedPack, filter);
        }
        List<String> paths = ImmutableList.of();
        if (pack instanceof DefaultResourcePack) {
            try (FileSystem fs = obtainFileSystem(DefaultResourcePack.class)) {
                Path basePath = fs.getPath("/assets");
                try (Stream<Path> stream = Files.walk(basePath)) {
                    paths = stream.map(Path::toString)
                        .filter(filter)
                        .collect(Collectors.toList());
                }
            }
        } else if (pack instanceof FileResourcePack) {
            ZipFile zf;
            try {
                zf = (ZipFile) GET_FILE_PACK_ZIP_FILE.invoke(pack);
            } catch (ReflectiveOperationException e) {
                throw new IOException(e);
            }
            paths = zf.stream().map(ZipEntry::getName).filter(filter).collect(Collectors.toList());
        } else {
            VintageFix.LOGGER.warn("Cannot list resources from pack {} ({})", pack.getPackName(), pack.getClass().getName());
        }
        return paths;
    }

    public static final Pattern PATH_TO_RESLOC_REGEX = Pattern.compile("^/?assets/(.+?(?=/))/(.*)$");
    public static final Pattern PATH_TO_SHORT_RESLOC_REGEX = Pattern.compile("^^/?assets/(.+?(?=/))/(?:.+?(?=/))/(.*)\\.(?:[A-Za-z]*)$");

    public enum ResourceLocationMatchType {
        SHORT,
        FULL
    }

    public static ResourceLocation pathToResourceLocation(String path, ResourceLocationMatchType type) {
        Pattern pattern;
        switch(type) {
            default:
            case SHORT:
                pattern = PATH_TO_SHORT_RESLOC_REGEX;
                break;
            case FULL:
                pattern = PATH_TO_RESLOC_REGEX;
                break;
        }
        Matcher matcher = pattern.matcher(path);
        if (matcher.matches())
            return new ResourceLocation(matcher.group(1), matcher.group(2));
        else
            return null;
    }
}
