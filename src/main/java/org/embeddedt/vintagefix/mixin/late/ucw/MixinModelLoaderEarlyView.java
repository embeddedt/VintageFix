package org.embeddedt.vintagefix.mixin.late.ucw;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModel;
import org.embeddedt.vintagefix.dynamicresources.model.DynamicModelProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import pl.asie.ucw.util.ModelLoaderEarlyView;

import java.util.Map;

@Mixin(ModelLoaderEarlyView.class)
public class MixinModelLoaderEarlyView {
    @Redirect(method = "getModel(Lnet/minecraft/client/renderer/block/model/ModelResourceLocation;)Lnet/minecraftforge/client/model/IModel;", at = @At(value = "INVOKE", target = "Ljava/util/Map;get(Ljava/lang/Object;)Ljava/lang/Object;"), remap = false)
    private Object useVintageSecretSauce(Map map, Object location) {
        // slightly more... vintage sauce
        return DynamicModelProvider.instance.getObject((ResourceLocation)location);
    }

    @Redirect(method = "putModel", at = @At(value = "INVOKE", target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"), remap = false)
    private Object useVintageSecretSauce(Map map, Object location, Object model) {
        // slightly more... vintage sauce
        DynamicModelProvider.instance.putObject((ResourceLocation)location, (IModel)model);
        return null;
    }
}
