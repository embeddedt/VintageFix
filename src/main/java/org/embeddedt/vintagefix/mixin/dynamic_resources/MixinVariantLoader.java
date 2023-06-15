package org.embeddedt.vintagefix.mixin.dynamic_resources;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.block.model.ModelBlockDefinition;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.model.VariantList;
import net.minecraft.client.renderer.block.model.multipart.Multipart;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModel;
import org.embeddedt.vintagefix.annotation.ClientOnlyMixin;
import org.embeddedt.vintagefix.dynamicresources.model.ModelLocationInformation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Mixin(targets = "net/minecraftforge/client/model/ModelLoader$VariantLoader")
@ClientOnlyMixin
public class MixinVariantLoader {
    private Cache<ResourceLocation, ModelBlockDefinition> modelBlockDefinitionCache =
        CacheBuilder.newBuilder()
            .expireAfterAccess(2, TimeUnit.MINUTES)
            .maximumSize(100)
            .concurrencyLevel(8)
            .softValues()
            .build();

    private static final MethodHandle WEIGHTED_CONSTRUCTOR, MULTIPART_CONSTRUCTOR;

    static {
        MethodHandle variant, multipart;
        try {
            Class<?> weightedModel = Class.forName("net.minecraftforge.client.model.ModelLoader$WeightedRandomModel");
            Class<?> multipartModel = Class.forName("net.minecraftforge.client.model.ModelLoader$MultipartModel");
            Constructor<?> weightedC = weightedModel.getConstructor(ResourceLocation.class, VariantList.class);
            Constructor<?> multipartC = multipartModel.getConstructor(ResourceLocation.class, Multipart.class);
            variant = MethodHandles.lookup().unreflectConstructor(weightedC);
            multipart = MethodHandles.lookup().unreflectConstructor(multipartC);
        } catch(ReflectiveOperationException | RuntimeException e) {
            variant = null;
            multipart = null;
        }
        WEIGHTED_CONSTRUCTOR = variant;
        MULTIPART_CONSTRUCTOR = multipart;
    }

    @Inject(method = "onResourceManagerReload", at = @At("HEAD"))
    private void onReload(IResourceManager manager, CallbackInfo ci) {
        modelBlockDefinitionCache.invalidateAll();
    }

    private VariantList normalizeAndGetVariant(ModelBlockDefinition definition, String variant) {
        if(definition.hasVariant(variant))
            return definition.getVariant(variant);
        else if(variant.equals("normal") && definition.hasVariant(""))
            return definition.getVariant("");
        else
            return null;
    }

    @Overwrite(remap = false)
    public IModel loadModel(ResourceLocation modelLocation) throws Exception {
        ModelResourceLocation variant = (ModelResourceLocation) modelLocation;
        ModelBlockDefinition definition = vfix$getModelBlockDefinition(variant);
        VariantList vList = normalizeAndGetVariant(definition, variant.getVariant());

        if (vList != null) {
            try {
                return (IModel)WEIGHTED_CONSTRUCTOR.invoke(variant, vList);
            } catch(Throwable e) {
                throw (Exception)e;
            }
        } else {
            if (definition.hasMultipartData()) {
                ResourceLocation baseLocation = new ResourceLocation(variant.getNamespace(), variant.getPath());
                Block block = ModelLocationInformation.getBlockFromBlockstateLocation(baseLocation);
                if (block != null) {
                    if(!ModelLocationInformation.isAppropriateMultipart(baseLocation, variant))
                        throw new Exception("Not valid multipart for " + block + ": " + variant);
                    definition.getMultipartData().setStateContainer(block.getBlockState());
                }
            }
            try {
                return (IModel)MULTIPART_CONSTRUCTOR.invoke(new ResourceLocation(variant.getNamespace(), variant.getPath()), definition.getMultipartData());
            } catch(Throwable e) {
                throw (Exception)e;
            }
        }
    }

    private ModelBlockDefinition vfix$getModelBlockDefinition(ResourceLocation location) {
        ResourceLocation simpleLocation = new ResourceLocation(location.getNamespace(), location.getPath());
        try {
            return modelBlockDefinitionCache.get(simpleLocation, () -> ModelLocationInformation.loadModelBlockDefinition(simpleLocation));
        } catch (ExecutionException e) {
            throw new RuntimeException(e.getCause());
        }
    }
}
