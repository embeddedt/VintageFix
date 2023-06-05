package org.embeddedt.vintagefix.dynamicresources.helpers;

import net.minecraft.client.resources.AbstractResourcePack;
import net.minecraft.client.resources.FolderResourcePack;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import org.embeddedt.vintagefix.dynamicresources.ResourcePackHelper;
import org.embeddedt.vintagefix.util.Util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FolderPackAdapter implements ResourcePackHelper.Adapter<FolderResourcePack> {
    @Override
    public Iterator<String> getAllPaths(FolderResourcePack pack, Predicate<String> filter) throws IOException {
        File packFolder = ObfuscationReflectionHelper.getPrivateValue(AbstractResourcePack.class, (AbstractResourcePack)pack, "field_110597_b");
        Path basePath = packFolder.toPath();
        try(Stream<Path> stream = Files.walk(basePath)) {
            return stream.map(basePath::relativize).map(Util::normalizePathToString)
                .filter(filter)
                .collect(Collectors.toList())
                .iterator();
        }
    }
}
