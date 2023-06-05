package org.embeddedt.vintagefix.mixin.dynamic_resources;

import appeng.bootstrap.components.ModelOverrideComponent;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.embeddedt.vintagefix.annotation.ClientOnlyMixin;
import org.embeddedt.vintagefix.annotation.LateMixin;
import org.embeddedt.vintagefix.event.DynamicModelBakeEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;
import java.util.function.BiFunction;

@Mixin(ModelOverrideComponent.class)
@LateMixin
@ClientOnlyMixin
public class AutoRotatingModelMixin {
    @Shadow(remap = false)
    @Final
    private Map<String, BiFunction<ModelResourceLocation, IBakedModel, IBakedModel>> customizer;

    @SubscribeEvent
    public void onDynamicModelBake(DynamicModelBakeEvent e) {
        if (e.location instanceof ModelResourceLocation && e.location.getNamespace().equals("appliedenergistics2")) {
            IBakedModel orgModel = e.bakedModel;
            BiFunction<ModelResourceLocation, IBakedModel, IBakedModel> customizer = this.customizer.get(e.location.getPath());
            if (customizer != null) {
                IBakedModel newModel = customizer.apply((ModelResourceLocation)e.location, orgModel);
                if (newModel != orgModel) {
                    e.bakedModel = newModel;
                }
            }
        }
    }
}
