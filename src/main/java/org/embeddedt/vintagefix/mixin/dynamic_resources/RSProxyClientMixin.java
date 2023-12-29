package org.embeddedt.vintagefix.mixin.dynamic_resources;

import com.raoulvdberge.refinedstorage.proxy.ProxyClient;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.embeddedt.vintagefix.annotation.ClientOnlyMixin;
import org.embeddedt.vintagefix.annotation.LateMixin;
import org.embeddedt.vintagefix.event.DynamicModelBakeEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;
import java.util.function.Function;

@Mixin(ProxyClient.class)
@ClientOnlyMixin
@LateMixin
public class RSProxyClientMixin {
    @Shadow(remap = false)
    private Map<ResourceLocation, Function<IBakedModel, IBakedModel>> bakedModelOverrides;

    @SubscribeEvent
    public void onDynBake(DynamicModelBakeEvent event) {
        ResourceLocation key = new ResourceLocation(event.location.getNamespace(), event.location.getPath());
        Function<IBakedModel, IBakedModel> replacer = bakedModelOverrides.get(key);
        if(replacer != null) {
            IBakedModel replacement = replacer.apply(event.bakedModel);
            if(replacement != null)
                event.bakedModel = replacement;
        }
    }
}
