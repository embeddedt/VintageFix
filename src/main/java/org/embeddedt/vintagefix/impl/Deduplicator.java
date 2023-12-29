package org.embeddedt.vintagefix.impl;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import it.unimi.dsi.fastutil.HashCommon;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.embeddedt.vintagefix.hash.LambdaBasedHash;
import org.embeddedt.vintagefix.util.PredicateHelper;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.MultipartBakedModel;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.client.resources.SimpleReloadableResourceManager;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import org.embeddedt.vintagefix.VintageFix;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class Deduplicator {
    private static final Map<String, String> VARIANT_IDENTITIES = new ConcurrentHashMap<>();
    // Typedefs would be a nice thing to have
    private static final Map<Map<Predicate<IBlockState>, IBakedModel>, MultipartBakedModel> KNOWN_MULTIPART_MODELS = new ConcurrentHashMap<>();
    private static final Map<List<Predicate<IBlockState>>, Predicate<IBlockState>> OR_PREDICATE_CACHE = new ConcurrentHashMap<>();
    private static final Map<List<Predicate<IBlockState>>, Predicate<IBlockState>> AND_PREDICATE_CACHE = new ConcurrentHashMap<>();
    private static final ObjectOpenCustomHashSet<int[]> BAKED_QUAD_CACHE = new ObjectOpenCustomHashSet<>(
            new LambdaBasedHash<>(Deduplicator::betterIntArrayHash, Arrays::equals)
    );

    public static String deduplicateVariant(String variant) {
        return VARIANT_IDENTITIES.computeIfAbsent(variant, Function.identity());
    }

    /**
     * An alternative to Arrays::hashCode for int arrays that appears to be more collision resistant for baked quad
     * vertex data arrays. Arrays::hashCode seems to be prone to collisions when arrays only differ slightly; this
     * caused the slowdown observed in FerriteCore issue #129.
     */
    private static int betterIntArrayHash(int[] in) {
        int result = 0;
        for (int i : in) {
            result = 31 * result + HashCommon.murmurHash3(i);
        }
        return result;
    }

    public static MultipartBakedModel makeMultipartModel(Map<Predicate<IBlockState>, IBakedModel> selectors) {
        return KNOWN_MULTIPART_MODELS.computeIfAbsent(ImmutableMap.copyOf(selectors), MultipartBakedModel::new);
    }

    public static Predicate<IBlockState> or(List<Predicate<IBlockState>> list) {
        return OR_PREDICATE_CACHE.computeIfAbsent(list, PredicateHelper::or);
    }

    public static Predicate<IBlockState> and(List<Predicate<IBlockState>> list) {
        return AND_PREDICATE_CACHE.computeIfAbsent(list, PredicateHelper::and);
    }

    private static final MethodHandle BAKED_QUAD_VERTEX_FIELD;
    static {
        try {
            Field field = ObfuscationReflectionHelper.findField(BakedQuad.class, "field_178215_a");
            field.setAccessible(true);
            BAKED_QUAD_VERTEX_FIELD = MethodHandles.lookup().unreflectSetter(field);
        } catch(ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public static void deduplicate(BakedQuad bq) {
        synchronized (BAKED_QUAD_CACHE) {
            int[] deduped = BAKED_QUAD_CACHE.addOrGet(bq.getVertexData());
            try {
                BAKED_QUAD_VERTEX_FIELD.invokeExact(bq, deduped);
            } catch(Throwable e) {
                VintageFix.LOGGER.error("Cannot deduplicate BakedQuad", e);
            }
        }
    }

    public static void registerReloadListener() {
        // Register the reload listener s.t. its "sync" part runs after the model loader reload
        ((SimpleReloadableResourceManager)Minecraft.getMinecraft().getResourceManager()).registerReloadListener(new IResourceManagerReloadListener() {
            @Override
            public void onResourceManagerReload(IResourceManager resourceManager) {
                VARIANT_IDENTITIES.clear();
                KNOWN_MULTIPART_MODELS.clear();
                OR_PREDICATE_CACHE.clear();
                AND_PREDICATE_CACHE.clear();
                synchronized (BAKED_QUAD_CACHE) {
                    BAKED_QUAD_CACHE.clear();
                    BAKED_QUAD_CACHE.trim();
                }
            }
        });
    }
}
