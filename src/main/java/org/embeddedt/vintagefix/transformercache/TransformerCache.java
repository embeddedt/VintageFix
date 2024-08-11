package org.embeddedt.vintagefix.transformercache;

import static org.embeddedt.vintagefix.VintageFix.LOGGER;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.unsafe.UnsafeInput;
import com.esotericsoftware.kryo.unsafe.UnsafeOutput;
import com.google.common.hash.Hashing;

import net.minecraft.launchwrapper.IClassNameTransformer;
import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;
import net.minecraftforge.fml.common.asm.ASMTransformerWrapper;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import org.apache.commons.io.FileUtils;
import org.embeddedt.vintagefix.VintageFix;
import org.embeddedt.vintagefix.util.Reflector;
import org.embeddedt.vintagefix.util.Util;

/* Format:
 * int8 0
 * int8 version
 * Map<String, TransformerData> map
 */
public class TransformerCache {

    public static TransformerCache instance = new TransformerCache();

    private List<IClassTransformer> myTransformers = new ArrayList<>();
    private Map<String, TransformerData> transformerMap = new HashMap<>();

    private static final byte MAGIC_0 = 0;
    private static final byte VERSION = 1;

    private static final File DAT_OLD = Util.childFile(VintageFix.CACHE_DIR, "transformerCache.dat");
    private static final File DAT = Util.childFile(VintageFix.CACHE_DIR, "classTransformerLite.cache");
    private static final File DAT_ERRORED = Util.childFile(VintageFix.CACHE_DIR, "classTransformerLite.cache.errored");
    private static final File TRANSFORMERCACHE_PROFILER_CSV = Util.childFile(VintageFix.OUT_DIR, "transformercache_profiler.csv");
    private final Kryo kryo = new Kryo();

    private Set<Class<?>> transformersToCache = new HashSet<>();

    private boolean inited = false;

    private static byte[] memoizedHashData;
    private static int memoizedHashValue;

    private static final String[] TRANSFORMER_LIST = new String[] {
        "net.minecraftforge.fml.common.asm.transformers.DeobfuscationTransformer",
        "net.minecraftforge.fml.common.asm.transformers.FieldRedirectTransformer",
        "net.minecraftforge.fml.common.asm.transformers.SideTransformer",
        "net.minecraftforge.fml.common.asm.transformers.TerminalTransformer",
        "git.jbredwards.fluidlogged_api.mod.asm.transformers.TransformerLevelProperty"
    };

    private static final int MAX_SIZE_MB = 256;

    public void init() {
        if(inited) return;

        transformersToCache = Arrays.stream(TRANSFORMER_LIST).map(clzName -> {
            try {
                return Class.forName(clzName, false, TransformerCache.class.getClassLoader());
            } catch(ClassNotFoundException e) {
                return null;
            }
        }).filter(Objects::nonNull).collect(Collectors.toSet());

        // We get a ClassCircularityError if we don't add this
        Launch.classLoader.addTransformerExclusion("org.embeddedt.vintagefix.transformercache");

        loadData();

        hookClassLoader();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                try {
                    saveTransformerCache();
                    saveProfilingResults();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        inited = true;
    }

    public void printStats() {
        if(!this.inited)
            return;
        long usedSpace = 0;
        for(TransformerData transData : transformerMap.values()) {
            for(TransformerData.CachedTransformation transformation : transData.transformationMap.values()) {
                usedSpace += transformation.getEstimatedSize();
            }
        }

        LOGGER.info("Transformation cache usage: {}/{}", FileUtils.byteCountToDisplaySize(usedSpace), FileUtils.byteCountToDisplaySize(MAX_SIZE_MB * 1024L * 1024L));
    }

    private static final Field TRANSFORMER_WRAPPER_PARENT_FIELD = Reflector.findField(ASMTransformerWrapper.TransformerWrapper.class, "parent");

    private static IClassTransformer getWrappedParent(IClassTransformer transformer) {
        try {
            return (IClassTransformer)TRANSFORMER_WRAPPER_PARENT_FIELD.get(transformer);
        } catch(ReflectiveOperationException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String getUniqueTransformerName(IClassTransformer transformer) {
        if(transformer instanceof ASMTransformerWrapper.TransformerWrapper) {
            IClassTransformer wrapped = getWrappedParent(transformer);
            if(wrapped != null)
                return wrapped.getClass().getCanonicalName();
            else
                return "UNKNOWN.ASM";
        } else
            return transformer.getClass().getCanonicalName();
    }

    private void hookClassLoader() {
        LaunchClassLoader lcl = (LaunchClassLoader)Launch.classLoader;
        List<IClassTransformer> transformers = (List<IClassTransformer>) ReflectionHelper.getPrivateValue(LaunchClassLoader.class, lcl, "transformers");
        for(int i = 0; i < transformers.size(); i++) {
            IClassTransformer transformer = transformers.get(i);
            IClassTransformer matchAgainst = transformer;
            if(transformer instanceof ASMTransformerWrapper.TransformerWrapper) {
                IClassTransformer wrapped = getWrappedParent(transformer);
                if(wrapped != null)
                    matchAgainst = wrapped;
            }
            boolean matches = false;
            for(Class<?> clz : transformersToCache) {
                if(clz.isAssignableFrom(matchAgainst.getClass())) {
                    matches = true;
                    break;
                }
            }
            if(matches) {
                LOGGER.info("Replacing " + getUniqueTransformerName(transformer) + " with cached proxy");

                IClassTransformer newTransformer = transformer instanceof IClassNameTransformer
                        ? new CachedNameTransformerProxy(transformer) : new CachedTransformerProxy(transformer);

                myTransformers.add(newTransformer);
                transformers.set(i, newTransformer);
            }
        }
    }

    private void loadData() {
        kryo.setRegistrationRequired(false);

        if(DAT_OLD.exists() && !DAT.exists()) {
            LOGGER.info("Migrating class cache: " + DAT_OLD + " -> " + DAT);
            DAT_OLD.renameTo(DAT);
        }

        if(DAT.exists()) {
            try(Input is = new UnsafeInput(new BufferedInputStream(new FileInputStream(DAT)))) {
                byte magic0 = kryo.readObject(is, byte.class);
                byte version = kryo.readObject(is, byte.class);

                if(magic0 != MAGIC_0 || version != VERSION) {
                    LOGGER.warn("Transformer cache is either a different version or corrupted, discarding.");
                } else {
                    transformerMap = returnVerifiedTransformerMap(kryo.readObject(is, HashMap.class));
                }

                for(TransformerData data : transformerMap.values()) {
                    if(data.transformerClassName.startsWith("$wrapper."))
                        data.transformerClassName = data.transformerClassName.substring(9);
                    boolean keep = false;
                    try {
                        Class<?> clz = Class.forName(data.transformerClassName, false, TransformerCache.class.getClassLoader());
                        for(Class<?> potentialSuper : transformersToCache) {
                            if(potentialSuper.isAssignableFrom(clz)) {
                                keep = true;
                                break;
                            }
                        }
                    } catch(ClassNotFoundException e) {
                        keep = false;
                    }
                    if(!keep) {
                        LOGGER.info("Dropping " + data.transformerClassName + " from cache because we don't care about it anymore.");
                    } else
                        LOGGER.info("Loaded cached data for {}", data.transformerClassName);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch(Exception e) {
                LOGGER.error("There was an error reading the transformer cache. A new one will be created. The previous one has been saved as " + DAT_ERRORED.getName() + " for inspection.");
                DAT.renameTo(DAT_ERRORED);
                e.printStackTrace();
            }
        }
    }

    private static Map<String, TransformerData> returnVerifiedTransformerMap(Map<String, TransformerData> map) {
        if(map.containsKey(null)) {
            throw new RuntimeException("Map contains null key");
        }
        if(map.containsValue(null)) {
            throw new RuntimeException("Map contains null value");
        }
        for(String key : map.keySet()) {
            if(!Util.isValidClassName(key)) {
                throw new RuntimeException("Map contains invalid key: " + key);
            }
        }
        return map;
    }

    private void saveTransformerCache() throws IOException {
        if(!DAT.exists()) {
            DAT.getParentFile().mkdirs();
            DAT.createNewFile();
        }
        LOGGER.info("Saving transformer cache");
        trimCache((long)MAX_SIZE_MB * 1024l * 1024l);
        try(Output output = new UnsafeOutput(new BufferedOutputStream(new FileOutputStream(DAT)))) {
            kryo.writeObject(output, MAGIC_0);
            kryo.writeObject(output, VERSION);
            kryo.writeObject(output, transformerMap);
        }
    }

    private void trimCache(long maxSize) {
        if(maxSize == -1) return;

        List<TransformerData.CachedTransformation> data = new ArrayList<>();

        for(TransformerData transData : transformerMap.values()) {
            data.addAll(transData.transformationMap.values());
        }

        data.sort(this::sortByAge);

        long usedSpace = 0;
        int cutoff = -1;
        for(int i = data.size() - 1; i >= 0; i--) {
            usedSpace += data.get(i).getEstimatedSize();
            if(usedSpace > maxSize) {
                cutoff = data.get(i).lastAccessed;
                break;
            }
        }

        if(cutoff != -1) {
            final int cutoffCopy = cutoff;
            for(TransformerData transData : transformerMap.values()) {
                transData.transformationMap.entrySet().removeIf(e -> e.getValue().lastAccessed <= cutoffCopy);
            }
            transformerMap.entrySet().removeIf(e -> e.getValue().transformationMap.isEmpty());
        }
    }

    private int sortByAge(TransformerData.CachedTransformation a, TransformerData.CachedTransformation b) {
        return a.lastAccessed < b.lastAccessed ? -1 : a.lastAccessed > b.lastAccessed ? 1 : 0;
    }

    private void saveProfilingResults() throws IOException {
        try(FileWriter fw = new FileWriter(TRANSFORMERCACHE_PROFILER_CSV)){
            fw.write("class,name,runs,misses\n");
            for(IClassTransformer transformer : myTransformers) {
                String className = getUniqueTransformerName(transformer);
                String name = transformer.toString();
                int runs = 0;
                int misses = 0;
                if(transformer instanceof CachedTransformerProxy) {
                    CachedTransformerProxy proxy = (CachedTransformerProxy)transformer;
                    runs = proxy.runs;
                    misses = proxy.misses;
                }
                fw.write(className + "," + name + "," + runs + "," + misses + "\n");
            }
        }
    }

    public byte[] getCached(String transName, String name, String transformedName, byte[] basicClass) {
        TransformerData transData = transformerMap.get(transName);
        if(transData != null) {
            TransformerData.CachedTransformation trans = transData.transformationMap.get(transformedName);
            if(trans != null) {
                if(nullSafeLength(basicClass) == trans.preLength && calculateHash(basicClass) == trans.preHash) {
                    trans.lastAccessed = now();
                    return trans.postHash == trans.preHash ? basicClass : trans.newClass;
                }
            }
        }
        return null;
    }

    private static int nullSafeLength(byte[] array) {
        return array == null ? -1 : array.length;
    }

    public void prePutCached(String transName, String name, String transformedName, byte[] basicClass) {
        TransformerData data = transformerMap.get(transName);
        if(data == null) {
            transformerMap.put(transName, data = new TransformerData(transName));
        }
        data.transformationMap.put(transformedName, new TransformerData.CachedTransformation(transformedName, calculateHash(basicClass), nullSafeLength(basicClass)));
    }

    /** MUST be preceded with a call to prePutCached. */
    public void putCached(String transName, String name, String transformedName, byte[] result) {
        transformerMap.get(transName).transformationMap.get(transformedName).putClass(result);
    }

    public static int calculateHash(byte[] data) {
        if(data == memoizedHashData) {
            return memoizedHashValue;
        }
        memoizedHashData = data;
        memoizedHashValue = data == null ? -1 : Hashing.adler32().hashBytes(data).asInt();
        return memoizedHashValue;
    }

    private static int now() {
        // TODO update the format in 6055
        return (int)(System.currentTimeMillis() / 1000 / 60);
    }

    public static class TransformerData {
        String transformerClassName;
        Map<String, CachedTransformation> transformationMap = new HashMap<>();

        public TransformerData(String transformerClassName) {
            this.transformerClassName = transformerClassName;
        }

        public TransformerData() {}

        public static class CachedTransformation {
            String targetClassName;
            int preLength;
            int preHash;
            int postHash;
            byte[] newClass;
            int lastAccessed;

            public CachedTransformation() {}

            public CachedTransformation(String targetClassName, int preHash, int preLength) {
                this.targetClassName = targetClassName;
                this.preHash = preHash;
                this.preLength = preLength;
                this.lastAccessed = now();
            }

            public void putClass(byte[] result) {
                postHash = calculateHash(result);
                if(preHash != postHash) {
                    newClass = result;
                }
            }

            public int getEstimatedSize() {
                return targetClassName.length() + 4 + 4 + 4 + (newClass != null ? newClass.length : 0) + 4;
            }
        }
    }
}
