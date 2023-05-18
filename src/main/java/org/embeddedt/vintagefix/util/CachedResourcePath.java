package org.embeddedt.vintagefix.util;

import com.google.common.base.Splitter;
import com.google.common.collect.Interner;
import com.google.common.collect.Interners;

import java.util.Arrays;

public class CachedResourcePath {
    private final String[] pathComponents;

    public static final Interner<String> PATH_COMPONENT_INTERNER = Interners.newStrongInterner();
    private static final Splitter SLASH_SPLITTER = Splitter.on('/');

    /**
     * Normalize a path by removing double slashes, etc.
     * <p></p>
     * This implementation avoids creating a new string unless there are actually double slashes present
     * in the input path.
     * @param path input path
     * @return a normalized version of the path
     */
    public static String normalize(String path) {
        char prevChar = 0;
        StringBuilder sb = null;
        for(int i = 0; i < path.length(); i++) {
            char thisChar = path.charAt(i);
            if(prevChar != '/' || thisChar != prevChar) {
                /* This character should end up in the final string. If we are using the builder, add it there. */
                if(sb != null)
                    sb.append(thisChar);
            } else {
                /* This character should not end up in the final string. We need to make a buidler if we haven't
                 * done so yet.
                 */
                if(sb == null) {
                    sb = new StringBuilder(path.length());
                    sb.append(path, 0, i);
                }
            }
            prevChar = thisChar;
        }
        return sb == null ? path : sb.toString();
    }

    public CachedResourcePath(String path, boolean intern) {
        int numComponents = 1;
        path = normalize(path);
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
}
