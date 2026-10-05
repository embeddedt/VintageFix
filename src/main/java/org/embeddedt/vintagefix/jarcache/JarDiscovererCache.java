package org.embeddedt.vintagefix.jarcache;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.unsafe.UnsafeInput;
import com.esotericsoftware.kryo.unsafe.UnsafeOutput;
import net.minecraftforge.fml.common.discovery.asm.ASMModParser;
import net.minecraftforge.fml.common.discovery.asm.ModAnnotation;
import org.embeddedt.vintagefix.VintageFix;
import org.embeddedt.vintagefix.util.Util;
import org.objectweb.asm.Type;
import sun.misc.Unsafe;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.*;
import java.util.zip.ZipEntry;

import static org.embeddedt.vintagefix.VintageFix.LOGGER;

/*
 * Format (v1):
 * int8 0
 * int8 version
 * int32 epoch
 * Map<String, CachedModInfo> cache
 */
@SuppressWarnings({"unchecked", "ResultOfMethodCallIgnored"})
public class JarDiscovererCache {

    /**
     * Max age of an element in the cache.
     */
    private static final int MAX_AGE = 8;

    private static Map<String, CachedModInfo> cache = new HashMap<>();
    private static int epoch;

    private static final byte MAGIC_0 = 0;
    private static final byte VERSION = 2;

    private static final File DAT_OLD = Util.childFile(VintageFix.CACHE_DIR, "jarDiscovererCache.dat");
    private static final File DAT = Util.childFile(VintageFix.CACHE_DIR, "jarDiscoverer.cache");
    private static final File DAT_ERRORED = Util.childFile(VintageFix.CACHE_DIR, "jarDiscoverer.cache.errored");

    private static final Kryo kryo = new Kryo();

    public static void load() {

        LOGGER.info("Loading JarDiscovererCache");

        try {
            kryo.register(Type.class, new TypeSerializer());
            kryo.register(ModAnnotation.class, new ModAnnotationSerializer());
            kryo.register(ModAnnotation.EnumHolder.class, new EnumHolderSerializer());
            kryo.register(ASMModParser.class, new ASMModParserSerializer());
            kryo.setRegistrationRequired(false);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            LOGGER.error("There was an error registering the jar discoverer cache serializers", e);
            return;
        }

        if(DAT_OLD.exists() && !DAT.exists()) {
            LOGGER.info("Migrating jar discoverer cache: {} -> {}", DAT_OLD, DAT);
            DAT_OLD.renameTo(DAT);
        }

        if(!DAT.exists()) {
            return;
        }

        try(Input is = new UnsafeInput(new BufferedInputStream(Files.newInputStream(DAT.toPath())))) {

            byte magic0 = kryo.readObject(is, byte.class);
            byte version = kryo.readObject(is, byte.class);
            epoch = kryo.readObject(is, int.class);
            epoch++;

            if(magic0 != MAGIC_0 || version != VERSION) {
                LOGGER.warn("Jar discoverer cache is either a different version or corrupted, discarding.");
            } else {
                cache = returnVerifiedMap(kryo.readObject(is, HashMap.class));
            }

        } catch (Exception e) {
            LOGGER.error("There was an error reading the jar discoverer cache. A new one will be created. The previous one has been saved as {} for inspection.", DAT_ERRORED.getName(), e);
            DAT.renameTo(DAT_ERRORED);
            cache.clear();
            epoch = 0;
        }

    }

    private static Map<String, CachedModInfo> returnVerifiedMap(Map<String, CachedModInfo> map) {
        if(map.containsKey(null)) {
            throw new RuntimeException("Map contains null key");
        }
        if(map.containsValue(null)) {
            throw new RuntimeException("Map contains null value");
        }
        return map;
    }

    public static void finish() {

        if(cache.isEmpty()) {
            return;
        }

        new Thread(() -> {

            try {

                if(!DAT.exists()) {
                    DAT.getParentFile().mkdirs();
                    DAT.createNewFile();
                }

                cache.entrySet().removeIf(e -> (epoch - e.getValue().lastAccessed) > MAX_AGE);

                try(Output output = new UnsafeOutput(new BufferedOutputStream(Files.newOutputStream(DAT.toPath())))) {
                    kryo.writeObject(output, MAGIC_0);
                    kryo.writeObject(output, VERSION);
                    kryo.writeObject(output, epoch);
                    kryo.writeObject(output, cache);
                }

            } catch (IOException e) {
                LOGGER.error("There was an error writing the jar discoverer cache", e);
            }

            cache = null;

        }, "JarDiscovererCache save thread").start();

    }

    public static CachedModInfo getCachedModInfo(String hash) {

        CachedModInfo cmi = cache.get(hash);

        if(cmi == null) {
            cmi = new CachedModInfo(true);
            cache.put(hash, cmi);
        }

        cmi.lastAccessed = epoch;

        return cmi;

    }

    public static boolean isActive() {
        return true;
    }

    public static class CachedModInfo {

        private final Map<String, ASMModParser> parserMap = new HashMap<>();
        private final Set<String> modClasses = new HashSet<>();
        private int lastAccessed;
        private final transient boolean dirty;

        public CachedModInfo(boolean dirty) {
            this.dirty = dirty;
        }

        public CachedModInfo() {
            this(false);
        }

        public ASMModParser getCachedParser(ZipEntry ze) {
            return parserMap.get(ze.getName());
        }

        public void putParser(ZipEntry ze, ASMModParser parser) {
            parserMap.put(ze.getName(), parser);
        }

        public int getCachedIsModClass(ZipEntry ze) {
            return dirty ? -1 : modClasses.contains(ze.getName()) ? 1 : 0;
        }

        public void putIsModClass(ZipEntry ze, boolean value) {

            if(!dirty) {
                throw new IllegalStateException();
            }

            if(value) {
                modClasses.add(ze.getName());
            }

        }

    }

    public static class TypeSerializer extends Serializer<Type> {

        @Override
        public void write(Kryo kryo, Output output, Type type) {

            output.writeByte(type.getSort());

            if(type.getSort() >= Type.ARRAY) {
                output.writeString(type.getInternalName());
            }

        }

        @Override
        public Type read(Kryo kryo, Input input, Class<? extends Type> type) {

            int sort = input.readByte();
            String buf = sort >= Type.ARRAY ? input.readString() : null;

            switch(sort) {
                case Type.VOID:
                    return Type.VOID_TYPE;
                case Type.BOOLEAN:
                    return Type.BOOLEAN_TYPE;
                case Type.CHAR:
                    return Type.CHAR_TYPE;
                case Type.BYTE:
                    return Type.BYTE_TYPE;
                case Type.SHORT:
                    return Type.SHORT_TYPE;
                case Type.INT:
                    return Type.INT_TYPE;
                case Type.FLOAT:
                    return Type.FLOAT_TYPE;
                case Type.LONG:
                    return Type.LONG_TYPE;
                case Type.DOUBLE:
                    return Type.DOUBLE_TYPE;
                case Type.ARRAY:
                case Type.OBJECT:
                    return Type.getObjectType(buf);
                case Type.METHOD:
                    return Type.getMethodType(buf);
                default:
                    return null;
            }

        }

    }

    public static class ModAnnotationSerializer extends Serializer<ModAnnotation> {

        private final Field typeField;

        public ModAnnotationSerializer() throws NoSuchFieldException {
            typeField = ModAnnotation.class.getDeclaredField("type");
            typeField.setAccessible(true);
        }

        @Override
        public void write(Kryo kryo, Output output, ModAnnotation ma) {
            kryo.writeObject(output, ma.getType());
            kryo.writeObject(output, ma.getASMType());
            output.writeString(ma.getMember());
            kryo.writeObject(output, ma.getValues());
        }

        @Override
        public ModAnnotation read(Kryo kryo, Input input, Class<? extends ModAnnotation> ma) {

            try {

                Object at = kryo.readObject(input, typeField.getType());
                ModAnnotation maa = new ModAnnotation(null, kryo.readObject(input, Type.class), input.readString());
                typeField.set(maa, at);

                try {
                    Map<String, Object> values = kryo.readObject(input, HashMap.class);
                    values.forEach(maa::addProperty);
                } catch(Exception e) {
                    return null;
                }

                return maa;

            } catch (SecurityException | IllegalArgumentException | IllegalAccessException e) {
                LOGGER.error("There was an error deserializing the jar discoverer cache ModAnnotation", e);
            }

            return null;

        }

    }

    public static class EnumHolderSerializer extends Serializer<ModAnnotation.EnumHolder> {

        private final Field descField;
        private final Field valueField;

        public EnumHolderSerializer() throws NoSuchFieldException {

            descField = ModAnnotation.EnumHolder.class.getDeclaredField("desc");
            descField.setAccessible(true);

            valueField = ModAnnotation.EnumHolder.class.getDeclaredField("value");
            valueField.setAccessible(true);

        }

        @Override
        public void write(Kryo kryo, Output output, ModAnnotation.EnumHolder eh) {

            try {
                output.writeString((String) descField.get(eh));
                output.writeString((String) valueField.get(eh));
            } catch (SecurityException | IllegalArgumentException | IllegalAccessException e) {
                LOGGER.error("There was an error serializing the jar discoverer cache ModAnnotation EnumHolder", e);
            }

        }

        @Override
        public ModAnnotation.EnumHolder read(Kryo kryo, Input input, Class<? extends ModAnnotation.EnumHolder> type) {
            return new ModAnnotation.EnumHolder(input.readString(), input.readString());
        }

    }

    public static class ASMModParserSerializer extends Serializer<ASMModParser> {

        private final Field asmTypeField;
        private final Field classVersionField;
        private final Field asmSuperTypeField;
        private final Field annotationsField;
        private final Field interfacesField;

        private final Unsafe unsafe;

        public ASMModParserSerializer() throws NoSuchFieldException, IllegalAccessException {

            asmTypeField = ASMModParser.class.getDeclaredField("asmType");
            asmTypeField.setAccessible(true);

            classVersionField = ASMModParser.class.getDeclaredField("classVersion");
            classVersionField.setAccessible(true);

            asmSuperTypeField = ASMModParser.class.getDeclaredField("asmSuperType");
            asmSuperTypeField.setAccessible(true);

            annotationsField = ASMModParser.class.getDeclaredField("annotations");
            annotationsField.setAccessible(true);

            interfacesField = ASMModParser.class.getDeclaredField("interfaces");
            interfacesField.setAccessible(true);

            Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            unsafe = (Unsafe) unsafeField.get(null);

        }

        @Override
        public void write(Kryo kryo, Output output, ASMModParser parser) {

            kryo.writeObjectOrNull(output, parser.getASMType(), Type.class);
            output.writeInt(parser.getClassVersion());

            kryo.writeObjectOrNull(output, parser.getASMSuperType(), Type.class);
            kryo.writeObject(output, parser.getAnnotations());

            try {
                kryo.writeObject(output, interfacesField.get(parser));
            } catch (IllegalAccessException e) {
                LOGGER.error("There was an error serializing the jar discoverer cache ASMModParser", e);
            }

        }

        @Override
        public ASMModParser read(Kryo kryo, Input input, Class<? extends ASMModParser> type) {

            try {

                ASMModParser parser = (ASMModParser) unsafe.allocateInstance(ASMModParser.class);

                asmTypeField.set(parser, kryo.readObjectOrNull(input, Type.class));
                classVersionField.setInt(parser, input.readInt());
                asmSuperTypeField.set(parser, kryo.readObjectOrNull(input, Type.class));

                annotationsField.set(parser, kryo.readObject(input, LinkedList.class));
                interfacesField.set(parser, kryo.readObject(input, HashSet.class));

                return parser;

            } catch (InstantiationException | IllegalAccessException e) {
                LOGGER.error("There was an error deserializing the jar discoverer cache ASMModParser", e);
            }

            return null;

        }

    }

}
