package org.embeddedt.vintagefix.dynamicresources.helpers;

import com.google.common.collect.ImmutableList;
import net.minecraft.client.resources.DefaultResourcePack;
import org.embeddedt.vintagefix.VintageFix;
import org.embeddedt.vintagefix.dynamicresources.ResourcePackHelper;
import org.embeddedt.vintagefix.util.Util;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipError;

public class DefaultPackAdapter implements ResourcePackHelper.Adapter<DefaultResourcePack> {
    @FunctionalInterface
    private interface FSConsumer {
        List<String> accept(FileSystem fs) throws IOException;
    }

    /**
     * Walk all file systems that the DefaultResourcePack would normally access. This includes the main mcassetsroot jar,
     * and any other jars on the classpath.
     * <p>
     * This method is synchronized to avoid multiple threads concurrently opening and closing filesystems, which can
     * cause weird filesystem not found errors.
     */
    private static synchronized Iterator<String> walkFileSystems(Class<?> clz, FSConsumer consumer) throws IOException {
        Set<String> set;
        FileSystem mainFs;
        try {
            URI uri = clz.getResource("/assets/.mcassetsroot").toURI();
            if ("jar".equals(uri.getScheme())) {
                try {
                    mainFs = FileSystems.getFileSystem(uri);
                } catch (FileSystemNotFoundException var11) {
                    mainFs = FileSystems.newFileSystem(uri, Collections.emptyMap());
                }
            } else
                throw new IOException("Wrong URI scheme: " + uri.getScheme());
        } catch (URISyntaxException e) {
            throw new IOException("Couldn't list vanilla resources", e);
        }
        set = new HashSet<>(consumer.accept(mainFs));
        mainFs.close();
        if(clz.getClassLoader() instanceof URLClassLoader) {
            URLClassLoader cl = (URLClassLoader)clz.getClassLoader();
            for(URL url : cl.getURLs()) {
                if(Objects.equals(url.getProtocol(), "asmgen")) continue; // Filter out protocols we don't understand
                List<String> newList = ImmutableList.of();
                URI uri = null;
                try {
                    // Decode the URL before passing it to the URI constructor so it isn't doubly encoded (which breaks
                    // reading files from folders with spaces)
                    uri = new URI("jar:" + url.toString());
                    boolean needClose = false;
                    FileSystem fs;
                    try {
                        fs = FileSystems.getFileSystem(uri);
                    } catch(FileSystemNotFoundException e) {
                        fs = FileSystems.newFileSystem(uri, Collections.emptyMap());
                        needClose = true;
                    }
                    try {
                        newList = consumer.accept(fs);
                    } catch(IOException ignored) {}
                    if(newList.size() > 0)
                        set.addAll(newList);
                    if(needClose)
                        fs.close();
                } catch(IOException | URISyntaxException | RuntimeException | ZipError e) {
                    VintageFix.LOGGER.error("Error accessing resource pack on classpath{}", (uri != null ? " (with URI " + uri + ")" : null), e);
                }
            }
        }
        return set.iterator();
    }

    @Override
    public Iterator<String> getAllPaths(DefaultResourcePack pack, Predicate<String> filter) throws IOException {
        return walkFileSystems(DefaultResourcePack.class, fs -> {
            Path basePath = fs.getPath("/assets");
            try (Stream<Path> stream = Files.walk(basePath)) {
                return stream.map(basePath::relativize).map(p -> "/assets/" + Util.normalizePathToString(p))
                    .filter(filter)
                    .collect(Collectors.toList());
            }
        });
    }
}
