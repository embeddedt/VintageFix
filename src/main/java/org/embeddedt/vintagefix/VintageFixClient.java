package org.embeddedt.vintagefix;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import org.embeddedt.vintagefix.impl.Deduplicator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.resources.*;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ColorHandlerEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class VintageFixClient {
    @SubscribeEvent
    public void registerListener(ColorHandlerEvent.Block event) {
        Deduplicator.registerReloadListener();
    }

    private static final Pattern TEXTURE_MATCH_PATTERN = Pattern.compile("^/?assets/(.+?(?=/))/textures/((?:attachment|bettergrass|block.?|cape|item.?|entity/(bed|chest)|pipe|ropebridge)/.*)\\.png$");

    @SubscribeEvent
    public void collectTextures(TextureStitchEvent.Pre event) {
        /* take every texture from these folders (1.19.3+ emulation) */
        Stopwatch watch = Stopwatch.createStarted();
        TextureMap map = event.getMap();
        Set<IResourcePack> resourcePacks = new LinkedHashSet<>();
        SimpleReloadableResourceManager manager = (SimpleReloadableResourceManager)Minecraft.getMinecraft().getResourceManager();
        Map<String, FallbackResourceManager> domainManagers = ObfuscationReflectionHelper.getPrivateValue(SimpleReloadableResourceManager.class, manager, "field_110548_a");
        for(FallbackResourceManager fallback : domainManagers.values()) {
            List<IResourcePack> fallbackPacks = ObfuscationReflectionHelper.getPrivateValue(FallbackResourceManager.class, fallback, "field_110540_a");
            resourcePacks.addAll(fallbackPacks);
        }
        for(IResourcePack pack : resourcePacks) {
            try {
                for(ResourceLocation texLoc : getAllResources(pack)) {
                    map.registerSprite(texLoc);
                }
            } catch(IOException e) {
                VintageFix.LOGGER.error("Error listing resources", e);
            }
        }
        watch.stop();
        VintageFix.LOGGER.info("Texture search took {}", watch);
    }

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
        } catch(URISyntaxException e) {
            throw new IOException("Couldn't list vanilla resources", e);
        }
    }

    private static final Method GET_FILE_PACK_ZIP_FILE = ObfuscationReflectionHelper.findMethod(FileResourcePack.class, "func_110599_c", ZipFile.class);

    private static Collection<ResourceLocation> getAllResources(IResourcePack pack) throws IOException {
        if(pack instanceof LegacyV2Adapter) {
            IResourcePack wrappedPack = ObfuscationReflectionHelper.getPrivateValue(LegacyV2Adapter.class, (LegacyV2Adapter)pack, "field_191383_a");
            return getAllResources(wrappedPack);
        }
        List<String> paths = ImmutableList.of();
        if(pack instanceof DefaultResourcePack) {
            try(FileSystem fs = obtainFileSystem(DefaultResourcePack.class)) {
                Path basePath = fs.getPath("/assets");
                try(Stream<Path> stream = Files.walk(basePath)) {
                    paths = stream.map(Path::toString)
                        .collect(Collectors.toList());
                }
            }
        } else if(pack instanceof FileResourcePack) {
            ZipFile zf;
            try {
                zf = (ZipFile)GET_FILE_PACK_ZIP_FILE.invoke(pack);
            } catch(ReflectiveOperationException e) {
                throw new IOException(e);
            }
            paths = zf.stream().map(ZipEntry::getName).collect(Collectors.toList());
        } else {
            VintageFix.LOGGER.warn("Cannot list resources from pack {} ({})", pack.getPackName(), pack.getClass().getName());
        }
        return paths.stream().flatMap(str -> {
            Matcher matcher = TEXTURE_MATCH_PATTERN.matcher(str);
            if(matcher.matches()) {
                return Stream.of(new ResourceLocation(matcher.group(1), matcher.group(2)));
            } else
                return Stream.of();
        }).collect(Collectors.toList());
    }
}
