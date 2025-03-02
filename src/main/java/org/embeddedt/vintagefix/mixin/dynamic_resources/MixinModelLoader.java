package org.embeddedt.vintagefix.mixin.dynamic_resources;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.minecraft.client.renderer.block.model.ModelBlockDefinition;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ModelLoader;
import org.embeddedt.vintagefix.VintageFix;
import org.embeddedt.vintagefix.annotation.ClientOnlyMixin;
import org.embeddedt.vintagefix.dynamicresources.IExtendedModelLoader;
import org.embeddedt.vintagefix.dynamicresources.model.ModelLocationInformation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Mixin(ModelLoader.class)
@ClientOnlyMixin
public class MixinModelLoader implements IExtendedModelLoader {
    private Cache<ResourceLocation, ModelBlockDefinition> modelBlockDefinitionCache =
        CacheBuilder.newBuilder()
            .expireAfterAccess(2, TimeUnit.MINUTES)
            .maximumSize(100)
            .concurrencyLevel(8)
            .softValues()
            .build();

    /**
     * @author embeddedt
     * @reason Use our code path for loading model block definitions, so that the state container is set correctly.
     */
    @Overwrite
    public ModelBlockDefinition getModelBlockDefinition(ResourceLocation location) {
        ResourceLocation simpleLocation = new ResourceLocation(location.getNamespace(), location.getPath());
        try {
            return modelBlockDefinitionCache.get(simpleLocation, () -> {
                try {
                    return ModelLocationInformation.loadModelBlockDefinition(simpleLocation);
                } catch(Exception e) {
                    VintageFix.LOGGER.debug("Error loading model block definition", e.getCause());
                    return new ModelBlockDefinition(new ArrayList<>());
                }
            });
        } catch (ExecutionException e) {
            throw new RuntimeException(e.getCause());
        }
    }

    @Override
    public ModelBlockDefinition vfix$getModelBlockDef(ResourceLocation location) {
        return getModelBlockDefinition(location);
    }
}
