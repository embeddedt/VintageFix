package org.embeddedt.vintagefix.mixin.dynamic_resources;

import net.minecraft.client.renderer.block.model.ModelBlockDefinition;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.model.VariantList;
import net.minecraft.client.renderer.block.model.multipart.Multipart;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoader;
import org.embeddedt.vintagefix.annotation.ClientOnlyMixin;
import org.embeddedt.vintagefix.dynamicresources.IExtendedModelLoader;
import org.embeddedt.vintagefix.dynamicresources.model.ModelLocationInformation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;

@Mixin(targets = "net/minecraftforge/client/model/ModelLoader$VariantLoader")
@ClientOnlyMixin
public class MixinVariantLoader {
    @Shadow
    private ModelLoader loader;

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

    private VariantList normalizeAndGetVariant(ModelBlockDefinition definition, String variant) {
        if(definition.hasVariant(variant))
            return definition.getVariant(variant);
        else if(variant.equals("normal") && definition.hasVariant(""))
            return definition.getVariant("");
        else
            return null;
    }

    /**
     * @author embeddedt, Runemoro
     * @reason use our model system
     */
    @Overwrite(remap = false)
    public IModel loadModel(ResourceLocation modelLocation) throws Exception {
        ModelResourceLocation variant = (ModelResourceLocation) modelLocation;
        ModelBlockDefinition definition = ((IExtendedModelLoader)(Object)this.loader).vfix$getModelBlockDef(variant);
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
                if(!ModelLocationInformation.isAppropriateMultipart(baseLocation, variant)) {
                    throw new Exception("Not valid multipart for " + definition.getMultipartData().getStateContainer() + ": " + variant);
                }
            }
            try {
                return (IModel)MULTIPART_CONSTRUCTOR.invoke(new ResourceLocation(variant.getNamespace(), variant.getPath()), definition.getMultipartData());
            } catch(Throwable e) {
                throw (Exception)e;
            }
        }
    }
}
