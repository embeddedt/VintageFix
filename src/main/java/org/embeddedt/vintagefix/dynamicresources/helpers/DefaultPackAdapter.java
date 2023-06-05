package org.embeddedt.vintagefix.dynamicresources.helpers;

import net.minecraft.client.resources.DefaultResourcePack;
import org.embeddedt.vintagefix.dynamicresources.ResourcePackHelper;
import org.embeddedt.vintagefix.util.Util;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.util.Collections;
import java.util.Iterator;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DefaultPackAdapter implements ResourcePackHelper.Adapter<DefaultResourcePack> {
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

    @Override
    public Iterator<String> getAllPaths(DefaultResourcePack pack, Predicate<String> filter) throws IOException {
        try (FileSystem fs = obtainFileSystem(DefaultResourcePack.class)) {
            Path basePath = fs.getPath("/assets");
            try (Stream<Path> stream = Files.walk(basePath)) {
                return stream.map(basePath::relativize).map(p -> "/assets/" + Util.normalizePathToString(p))
                    .filter(filter)
                    .collect(Collectors.toList())
                    .iterator();
            }
        }
    }
}
