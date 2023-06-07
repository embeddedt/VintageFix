package org.embeddedt.vintagefix.util;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

import java.io.File;

public class FolderPackCache {
    private final ObjectOpenHashSet<CachedResourcePath> cachedPaths = new ObjectOpenHashSet<>();
    public FolderPackCache(File folderPath) {
        explore(folderPath, "");
        cachedPaths.trim();
    }

    private void explore(File folder, String path) {
        File[] theFiles = folder.listFiles();
        if(theFiles == null)
            return;
        for(File f : theFiles) {
            String myPath = (path.isEmpty() ? "" : path + "/") + f.getName();
            CachedResourcePath cPath = new CachedResourcePath(myPath, true);
            cachedPaths.add(cPath);
            if(f.isDirectory()) {
                explore(f, myPath);
            }
        }
    }

    public boolean hasPath(String p) {
        return cachedPaths.contains(new CachedResourcePath(p, false));
    }
}
