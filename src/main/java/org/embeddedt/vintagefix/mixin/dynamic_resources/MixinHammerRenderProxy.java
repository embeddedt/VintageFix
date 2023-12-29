package org.embeddedt.vintagefix.mixin.dynamic_resources;

import com.zeitheron.hammercore.proxy.RenderProxy_Client;
import com.zeitheron.hammercore.utils.IdentityHashMapWC;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.embeddedt.vintagefix.annotation.ClientOnlyMixin;
import org.embeddedt.vintagefix.annotation.LateMixin;
import org.embeddedt.vintagefix.dynamicresources.IBlockModelShapes;
import org.embeddedt.vintagefix.dynamicresources.model.DynamicBakedModelProvider;
import org.embeddedt.vintagefix.event.DynamicModelBakeEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;

@Mixin(RenderProxy_Client.class)
@ClientOnlyMixin
@LateMixin
public class MixinHammerRenderProxy {
    @Shadow(remap = false)
    @Final
    public static IdentityHashMapWC<IBlockState, IBakedModel> bakedModelStore;

    private static HashMap<ResourceLocation, IBakedModel> bakedModelsByLocation = new HashMap<>();

    @Inject(method = "loadComplete", at = @At(value = "INVOKE", target = "Lcom/zeitheron/hammercore/utils/IdentityHashMapWC;putAll(Ljava/util/Map;)V", ordinal = 0, remap = false), cancellable = true, remap = false)
    private void divertIntoDynamicModels(CallbackInfo ci) {
        ci.cancel();
        IBlockModelShapes shapes = (IBlockModelShapes)Minecraft.getMinecraft().getBlockRendererDispatcher().getBlockModelShapes();
        bakedModelStore.constants.entrySet().forEach(entry -> {
            ModelResourceLocation location = shapes.getLocationForState(entry.getKey());
            bakedModelsByLocation.put(location, entry.getValue());
            DynamicBakedModelProvider.instance.invalidateThrough(location);
        });
    }

    @SubscribeEvent
    public void onDynamicModelBake(DynamicModelBakeEvent e) {
        IBakedModel m = bakedModelsByLocation.get(e.location);
        if(m != null)
            e.bakedModel = m;
    }
}
