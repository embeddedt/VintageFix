package org.embeddedt.vintagefix.util;

import com.google.common.base.Joiner;

import java.io.File;
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
}
