package org.embeddedt.vintagefix.mixin.dynamic_resources;

import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModel;
import org.embeddedt.vintagefix.VintageFix;
import org.embeddedt.vintagefix.annotation.ClientOnlyMixin;
import org.embeddedt.vintagefix.annotation.LateMixin;
import org.embeddedt.vintagefix.dynamicresources.model.DynamicModelProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.asie.ucw.util.ModelLoaderEarlyView;

import java.util.Map;

@Mixin(ModelLoaderEarlyView.class)
@LateMixin
@ClientOnlyMixin
public class MixinModelLoaderEarlyView {
    private IModel missing;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void storeMissing(CallbackInfo ci) {
        missing = DynamicModelProvider.instance.getObject(new ModelResourceLocation("builtin/missing", "missing"));
    }

    @Redirect(method = "getModel(Lnet/minecraft/client/renderer/block/model/ModelResourceLocation;)Lnet/minecraftforge/client/model/IModel;", at = @At(value = "INVOKE", target = "Ljava/util/Map;get(Ljava/lang/Object;)Ljava/lang/Object;"), remap = false)
    private Object useVintageSecretSauce(Map map, Object location) {
        // slightly more... vintage sauce
        try {
            return DynamicModelProvider.instance.getObject((ResourceLocation)location);
        } catch(RuntimeException e) {
            VintageFix.LOGGER.error("Error retrieving model {} for UCW: {}", location, e);
            return missing;
        }
    }

    @Redirect(method = "putModel", at = @At(value = "INVOKE", target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"), remap = false)
    private Object useVintageSecretSauce(Map map, Object location, Object model) {
        // slightly more... vintage sauce
        DynamicModelProvider.instance.putObject((ResourceLocation)location, (IModel)model);
        return null;
    }
}
