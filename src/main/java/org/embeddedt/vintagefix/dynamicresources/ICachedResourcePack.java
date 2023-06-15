package org.embeddedt.vintagefix.dynamicresources;

import javax.annotation.Nullable;
import java.util.stream.Stream;

public interface ICachedResourcePack {
    @Nullable
    Stream<String> getAllPaths();
}
