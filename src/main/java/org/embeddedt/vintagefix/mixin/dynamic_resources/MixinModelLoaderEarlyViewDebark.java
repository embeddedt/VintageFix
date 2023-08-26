package org.embeddedt.vintagefix.mixin.dynamic_resources;

import com.google.common.collect.Maps;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraftforge.client.model.IModel;
import org.embeddedt.vintagefix.annotation.LateMixin;
import org.embeddedt.vintagefix.dynamicresources.model.DynamicModelProvider;
import org.embeddedt.vintagefix.dynamicresources.model.ModelLocationInformation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Pseudo
@Mixin(targets = { "pl/asie/debark/util/ModelLoaderEarlyView"})
@LateMixin
public class MixinModelLoaderEarlyViewDebark {
    @Shadow(remap = false)
    private Map<ModelResourceLocation, IModel> secretSauce = null;

    @Inject(method = "<init>", at = @At("RETURN") , remap = false)
    private void changeBackingMap(CallbackInfo ci) {
        this.secretSauce = Maps.asMap(ModelLocationInformation.allKnownModelLocations, location -> DynamicModelProvider.instance.getModelOrMissing(location));
    }

}
