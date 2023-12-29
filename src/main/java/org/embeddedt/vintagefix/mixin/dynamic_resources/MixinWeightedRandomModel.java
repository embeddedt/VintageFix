package org.embeddedt.vintagefix.mixin.dynamic_resources;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import org.embeddedt.vintagefix.annotation.ClientOnlyMixin;
import org.embeddedt.vintagefix.dynamicresources.model.DynamicModelProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "net/minecraftforge/client/model/ModelLoader$WeightedRandomModel")
@ClientOnlyMixin
public class MixinWeightedRandomModel {
    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/client/model/ModelLoaderRegistry;getModelOrMissing(Lnet/minecraft/util/ResourceLocation;)Lnet/minecraftforge/client/model/IModel;", remap = false))
    private IModel skipModelRetrieval(ResourceLocation location) {
        if(DynamicModelProvider.textureCapturer != null)
            return ModelLoaderRegistry.getModelOrMissing(location); // need to actually retrieve so textures are captured
        else
            return null; /* no-op */
    }
}
