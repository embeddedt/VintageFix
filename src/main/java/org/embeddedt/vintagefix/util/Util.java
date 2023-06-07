package org.embeddedt.vintagefix.util;

import com.google.common.base.Joiner;
import org.embeddedt.vintagefix.VintageFix;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import javax.lang.model.SourceVersion;

public class Util {

    public static File childFile(File parent, String childName) {
        parent.mkdirs();
        return new File(parent, childName);
    }

    public static boolean isValidClassName(String className) {
        final String DOT_PACKAGE_INFO = ".package-info";
        if(className.endsWith(DOT_PACKAGE_INFO)) {
            className = className.substring(0, className.length() - DOT_PACKAGE_INFO.length());
        }
        return SourceVersion.isName(className);
    }

    private static final Joiner SLASH_JOINER = Joiner.on('/');

    public static String normalizePathToString(Path path) {
        return SLASH_JOINER.join(path);
    }

    public static String normalize(String path) {
        char prevChar = 0;
        StringBuilder sb = null;
        for(int i = 0; i < path.length(); i++) {
            char thisChar = path.charAt(i);
            if(thisChar == '\\')
                thisChar = '/';
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

    private static final boolean DEBUG_CANON = false;

    public static String getCanonicalPathFast(File file) throws IOException {
        String ours = Util.normalize(file.getAbsolutePath());
        if(DEBUG_CANON) {
            String theirs = file.getCanonicalPath();
            if(!theirs.equals(ours))
                VintageFix.LOGGER.warn("Path mismatch, expected {} got {}", theirs, ours);
        }
        return ours;
    }
}
