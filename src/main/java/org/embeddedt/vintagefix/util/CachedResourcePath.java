package org.embeddedt.vintagefix.util;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Interner;
import com.google.common.collect.Interners;

import java.util.Arrays;

public class CachedResourcePath {
    private final String[] pathComponents;

    public static final Interner<String> PATH_COMPONENT_INTERNER = Interners.newStrongInterner();
    private static final Splitter SLASH_SPLITTER = Splitter.on('/');

    public CachedResourcePath(String path, boolean intern) {
        int numComponents = 1;
        path = Util.normalize(path);
        for(int i = 0; i < path.length(); i++) {
            if(path.charAt(i) == '/')
                numComponents++;
        }
        String[] components = new String[numComponents];
        int i = 0;
        for(String s : SLASH_SPLITTER.split(path)) {
            components[i] = intern ? PATH_COMPONENT_INTERNER.intern(s) : s;
            i++;
        }
        pathComponents = components;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(pathComponents);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CachedResourcePath that = (CachedResourcePath) o;
        return Arrays.equals(pathComponents, that.pathComponents);
    }

    private static final Joiner JOINER = Joiner.on('/');

    @Override
    public String toString() {
        return JOINER.join(pathComponents);
    }
}
