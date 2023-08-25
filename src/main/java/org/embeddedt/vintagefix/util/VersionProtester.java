package org.embeddedt.vintagefix.util;

public class VersionProtester {
    public static String protest(String versionString) {
        return versionString.replaceAll("1\\.12\\.2", "12.2 LTS");
    }
}
