package org.embeddedt.vintagefix.classloading;

import com.google.common.base.Preconditions;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableMap;
import org.embeddedt.vintagefix.ducks.FastMapStateHolder;
import net.minecraft.block.properties.IProperty;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.function.Supplier;

/**
 * Helper to define classes in the com.google.common.collect package without issues due to jar signing and classloaders
 * (the second one only seems to be an issue on Fabric, but the first one is a problem on both)
 */
public class FastImmutableMapDefiner {
    private static final Logger LOGGER = LogManager.getLogger("VintageFix - class definer");

    public static String GOOGLE_ACCESS_PREFIX = "/googleaccess/";
    public static String GOOGLE_ACCESS_SUFFIX = ".class_manual";

    private static final Supplier<Definer> DEFINE_CLASS = Suppliers.memoize(() -> {
        try {
            // Try to create a Java 9+ style class definer
            // These are all public methods, but just don't exist in Java 8
            Method makePrivateLookup = MethodHandles.class.getMethod(
                "privateLookupIn", Class.class, MethodHandles.Lookup.class
            );
            Object privateLookup = makePrivateLookup.invoke(null, ImmutableMap.class, MethodHandles.lookup());
            Method defineClass = MethodHandles.Lookup.class.getMethod("defineClass", byte[].class);
            LOGGER.info("Using Java 9+ class definer");
            return (bytes, name) -> (Class<?>) defineClass.invoke(privateLookup, (Object) bytes);
        } catch (Exception x) {
            try {
                // If that fails, try a Java 8 style definer
                Method defineClass = ClassLoader.class.getDeclaredMethod(
                    "defineClass", String.class, byte[].class, int.class, int.class
                );
                defineClass.setAccessible(true);
                ClassLoader loader = ImmutableMap.class.getClassLoader();
                LOGGER.info("Using Java 8 class definer");
                return (bytes, name) -> (Class<?>) defineClass.invoke(loader, name, bytes, 0, bytes.length);
            } catch (NoSuchMethodException e) {
                // Fail if neither works
                throw new RuntimeException(e);
            }
        }
    });

    /**
     * Creates a MethodHandle for the constructor of FastMapEntryImmutableMap which takes one argument, which has to be
     * an instance FastMapStateHolder. This also handles the necessary classloader acrobatics.
     */
    private static final Supplier<MethodHandle> MAKE_IMMUTABLE_FAST_MAP = Suppliers.memoize(() -> {
        try {
            // Load these in the app classloader!
            defineInAppClassloader("com.google.common.collect.FerriteCoreEntrySetAccess");
            defineInAppClassloader("com.google.common.collect.FerriteCoreImmutableMapAccess");
            defineInAppClassloader("com.google.common.collect.FerriteCoreImmutableCollectionAccess");
            // This lives in the transforming classloader, but must not be loaded before the previous classes are in
            // the app classloader!
            Class<?> map = Class.forName("org.embeddedt.vintagefix.fastmap.immutable.FastMapEntryImmutableMap");
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            return lookup.findConstructor(map, MethodType.methodType(void.class, FastMapStateHolder.class));
        } catch (Exception x) {
            throw new RuntimeException(x);
        }
    });

    public static ImmutableMap<IProperty<?>, Comparable<?>> makeMap(FastMapStateHolder<?> state) {
        try {
            return (ImmutableMap<IProperty<?>, Comparable<?>>) MAKE_IMMUTABLE_FAST_MAP.get().invoke(state);
        } catch (Error e) {
            throw e;
        } catch (Throwable x) {
            throw new RuntimeException(x);
        }
    }

    private static void defineInAppClassloader(String name) throws Exception {
        byte[] classBytes;
        try (InputStream byteInput = FastImmutableMapDefiner.class.getResourceAsStream(
                GOOGLE_ACCESS_PREFIX + name.replace('.', '/') + GOOGLE_ACCESS_SUFFIX
        )) {
            Preconditions.checkNotNull(byteInput, "Failed to find class bytes for " + name);
            classBytes = IOUtils.toByteArray(byteInput);
        }
        Class<?> loaded = DEFINE_CLASS.get().define(classBytes, name);
        Preconditions.checkState(loaded.getClassLoader() == ImmutableMap.class.getClassLoader());
    }

    private interface Definer {
        Class<?> define(byte[] bytes, String name) throws Exception;
    }
}
