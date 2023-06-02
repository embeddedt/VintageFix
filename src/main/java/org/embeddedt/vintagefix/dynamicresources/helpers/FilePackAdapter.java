package org.embeddedt.vintagefix.dynamicresources.helpers;

import net.minecraft.client.resources.FileResourcePack;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import org.embeddedt.vintagefix.dynamicresources.ResourcePackHelper;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class FilePackAdapter implements ResourcePackHelper.Adapter<FileResourcePack> {
    private static final Method GET_FILE_PACK_ZIP_FILE = ObfuscationReflectionHelper.findMethod(FileResourcePack.class, "func_110599_c", ZipFile.class);

    @Override
    public Iterator<String> getAllPaths(FileResourcePack pack, Predicate<String> filter) throws IOException {
        ZipFile zf;
        try {
            zf = (ZipFile) GET_FILE_PACK_ZIP_FILE.invoke(pack);
        } catch (ReflectiveOperationException e) {
            throw new IOException(e);
        }
        return zf.stream().map(ZipEntry::getName).filter(filter).collect(Collectors.toList()).iterator();
    }
}
