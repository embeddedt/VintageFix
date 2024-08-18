package org.embeddedt.vintagefix.dynamicresources;

import com.google.common.base.Throwables;
import com.google.common.collect.Sets;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelBlock;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.embeddedt.vintagefix.VintageFix;
import org.embeddedt.vintagefix.event.DynamicModelBakeEvent;
import team.chisel.ctm.client.model.AbstractCTMBakedModel;
import team.chisel.ctm.client.texture.IMetadataSectionCTM;
import team.chisel.ctm.client.util.ResourceUtil;
import team.chisel.ctm.client.util.TextureMetadataHandler;

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

public class CTMHelper {
    private static final Object2BooleanMap<ResourceLocation> wrappedModels = ObfuscationReflectionHelper.getPrivateValue(TextureMetadataHandler.class, TextureMetadataHandler.INSTANCE, "wrappedModels");

    private static final Class<?> multipartModelClass;
    private static final Class<?> vanillaModelWrapperClass;
    private static final Field multipartPartModels;
    private static final Field modelWrapperModel;
    private static final MethodHandle wrapMethod;
    static {
        try {
            multipartModelClass = Class.forName("net.minecraftforge.client.model.ModelLoader$MultipartModel");
            multipartPartModels = multipartModelClass.getDeclaredField("partModels");
            multipartPartModels.setAccessible(true);
            vanillaModelWrapperClass = Class.forName("net.minecraftforge.client.model.ModelLoader$VanillaModelWrapper");
            modelWrapperModel = vanillaModelWrapperClass.getDeclaredField("model");
            modelWrapperModel.setAccessible(true);
            wrapMethod = MethodHandles.lookup().unreflect(ObfuscationReflectionHelper.findMethod(TextureMetadataHandler.class, "wrap", IBakedModel.class, IModel.class, IBakedModel.class));
        } catch (ReflectiveOperationException | SecurityException e) {
            throw Throwables.propagate(e);
        }
    }

    /**
     * Marker interface used to detect when an object is an instance of {@link AbstractCTMBakedModel} without needing
     * to load that class.
     */
    public static interface AbstractCTMModelInterface {

    }

    @SubscribeEvent
    public static void onDynModelBake(DynamicModelBakeEvent event) {
        if(!(event.location instanceof ModelResourceLocation))
            return;
        ModelResourceLocation mrl = (ModelResourceLocation)event.location;
        IModel rootModel = event.unbakedModel;
        if (rootModel != null) {
            if(event.bakedModel instanceof AbstractCTMModelInterface || event.bakedModel.isBuiltInRenderer())
                return;
            Deque<ResourceLocation> dependencies = new ArrayDeque<>();
            Set<ResourceLocation> seenModels = new HashSet<>();
            dependencies.push(mrl);
            seenModels.add(mrl);
            boolean shouldWrap = wrappedModels.getOrDefault(mrl, false);
            // Breadth-first loop through dependencies, exiting as soon as a CTM texture is found, and skipping duplicates/cycles
            while (!shouldWrap && !dependencies.isEmpty()) {
                ResourceLocation dep = dependencies.pop();
                IModel model;
                try {
                    model = dep == mrl ? rootModel : ModelLoaderRegistry.getModel(dep);
                } catch (Exception e) {
                    continue;
                }

                Set<ResourceLocation> textures = Sets.newHashSet(model.getTextures());
                // FORGE WHY
                if (vanillaModelWrapperClass.isAssignableFrom(model.getClass())) {
                    ModelBlock parent;
                    try {
                        parent = ((ModelBlock) modelWrapperModel.get(model)).parent;
                    } catch(ReflectiveOperationException e) {
                        throw new RuntimeException(e);
                    }
                    while (parent != null) {
                        textures.addAll(parent.textures.values().stream().filter(s -> !s.startsWith("#")).map(ResourceLocation::new).collect(Collectors.toSet()));
                        parent = parent.parent;
                    }
                }

                Set<ResourceLocation> newDependencies = Sets.newHashSet(model.getDependencies());

                // FORGE WHYYYYY
                if (multipartModelClass.isAssignableFrom(model.getClass())) {
                    Map<?, IModel> partModels;
                    try {
                        partModels = (Map<?, IModel>) multipartPartModels.get(model);
                    } catch(ReflectiveOperationException e) {
                        throw new RuntimeException(e);
                    }
                    textures = partModels.values().stream().map(m -> m.getTextures()).flatMap(Collection::stream).collect(Collectors.toSet());
                    newDependencies.addAll(partModels.values().stream().flatMap(m -> m.getDependencies().stream()).collect(Collectors.toList()));
                }

                for (ResourceLocation tex : textures) {
                    IMetadataSectionCTM meta = null;
                    try {
                        meta = ResourceUtil.getMetadata(ResourceUtil.spriteToAbsolute(tex));
                    } catch (IOException e) {} // Fallthrough
                    if (meta != null) {
                        shouldWrap = true;
                        break;
                    }
                }

                for (ResourceLocation rl : newDependencies) {
                    if (seenModels.add(rl)) {
                        dependencies.push(rl);
                    }
                }
            }
            wrappedModels.put(mrl, shouldWrap);
            if (shouldWrap) {
                try {
                    event.bakedModel = (IBakedModel)wrapMethod.invokeExact(TextureMetadataHandler.INSTANCE, rootModel, event.bakedModel);
                    dependencies.clear();
                } catch (Throwable e) {
                    VintageFix.LOGGER.error("Could not wrap model " + mrl + ". Aborting...", e);
                }
            }
        }
    }
}
