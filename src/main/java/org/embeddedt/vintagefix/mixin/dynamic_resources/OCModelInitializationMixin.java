package org.embeddedt.vintagefix.mixin.dynamic_resources;

import li.cil.oc.Settings$;
import li.cil.oc.client.renderer.block.ModelInitialization$;
import li.cil.oc.client.renderer.block.ScreenModel$;
import li.cil.oc.client.renderer.block.ServerRackModel;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.embeddedt.vintagefix.annotation.ClientOnlyMixin;
import org.embeddedt.vintagefix.annotation.LateMixin;
import org.embeddedt.vintagefix.event.DynamicModelBakeEvent;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ModelInitialization$.class)
@ClientOnlyMixin
@LateMixin
public class OCModelInitializationMixin {
    @SubscribeEvent
    public void onDynBake(DynamicModelBakeEvent e) {
        if(e.location.getNamespace().equals(Settings$.MODULE$.resourceDomain())) {
            String path = e.location.getPath();
            // I refuse to figure out what Scala did to the constants
            if(path.equals("screen1") || path.equals("screen2") || path.equals("screen3")) {
                e.bakedModel = ScreenModel$.MODULE$;
            } else if(path.equals("rack")) {
                e.bakedModel = new ServerRackModel(e.bakedModel);
            }
        }
    }
}
