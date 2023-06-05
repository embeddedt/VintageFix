package org.embeddedt.vintagefix.mixin.dynamicresources;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModel;
import org.embeddedt.vintagefix.annotation.ClientOnlyMixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "net/minecraftforge/client/model/ModelLoader$WeightedRandomModel")
@ClientOnlyMixin
public class MixinWeightedRandomModel {
    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/client/model/ModelLoaderRegistry;getModelOrMissing(Lnet/minecraft/util/ResourceLocation;)Lnet/minecraftforge/client/model/IModel;"))
    private IModel skipModelRetrieval(ResourceLocation location) {
        return null; /* no-op */
    }
}
