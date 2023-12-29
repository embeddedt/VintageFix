package org.embeddedt.vintagefix.mixin.dynamic_resources;

import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.embeddedt.vintagefix.annotation.ClientOnlyMixin;
import org.embeddedt.vintagefix.annotation.LateMixin;
import org.embeddedt.vintagefix.event.DynamicModelBakeEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import thebetweenlands.client.render.model.loader.CustomModelLoader;
import thebetweenlands.client.render.model.loader.extension.LoaderExtension;
import thebetweenlands.client.render.model.loader.extension.LoaderExtensionException;

import java.util.List;

@Mixin(CustomModelLoader.class)
@LateMixin
@ClientOnlyMixin
public abstract class MixinCustomBetweenlandsModels {
    @Shadow(remap = false)
    protected abstract void throwLoaderException(LoaderExtension extension, Throwable cause);

    @Shadow(remap = false)
    @Final
    private List<LoaderExtension> loaderExtensions;

    // handle model replacing for dynamically loaded models
    @SubscribeEvent
    public void onDynBake(DynamicModelBakeEvent event) {
        if(!(event.location instanceof ModelResourceLocation))
            return;
        IBakedModel previousModel = event.bakedModel;
        for(LoaderExtension extension : this.loaderExtensions) {
            try {
                IBakedModel replacement = extension.getModelReplacement((ModelResourceLocation)event.location, previousModel);
                if(replacement != null) {
                    event.bakedModel = replacement;
                }
            } catch(Exception ex) {
                if(!(ex instanceof LoaderExtensionException)) {
                    this.throwLoaderException(extension, ex);
                } else {
                    throw ex;
                }
            }
        }
    }
}
