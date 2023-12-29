package org.embeddedt.vintagefix.mixin.dynamic_resources;

import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelBlock;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.common.model.IModelState;
import org.embeddedt.vintagefix.annotation.ClientOnlyMixin;
import org.embeddedt.vintagefix.dynamicresources.model.DynamicModelProvider;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;

@Mixin(targets = "net/minecraftforge/client/model/ModelLoader$VanillaModelWrapper")
@ClientOnlyMixin
public abstract class MixinVanillaModelWrapper {
    @Shadow(remap = false)
    @Final
    private ModelBlock model;
    @Shadow(remap = false)
    public abstract IBakedModel bakeImpl(IModelState state, final VertexFormat format, Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter);

    @Redirect(method = "getDependencies", at = @At(value = "INVOKE", target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"), remap = false)
    private Object skipPutStateModel(Map map, Object k, Object v) {
        return null;
    }

    private static ModelResourceLocation invVariant(String s)
    {
        if(s.contains("#"))
        {
            return new ModelResourceLocation(s);
        }
        return new ModelResourceLocation(s, "inventory");
    }

    @Inject(method = "getDependencies", at = @At("RETURN"), remap = false)
    private void trackDeps(CallbackInfoReturnable<Collection<ResourceLocation>> cir) {
        for(ResourceLocation dep : model.getOverrideLocations()) {
            DynamicModelProvider.instance.putAlias(invVariant(dep.toString()), dep);
        }
    }

    @Redirect(method = "getDependencies", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/client/model/ModelLoaderRegistry;getModelOrLogError(Lnet/minecraft/util/ResourceLocation;Ljava/lang/String;)Lnet/minecraftforge/client/model/IModel;", remap = false), remap = false)
    private IModel skipLoadModel(ResourceLocation location, String s) {
        return null;
    }

    @Inject(method = "bake", at = @At("HEAD"), cancellable = true, remap = false)
    private void doDirectBake(IModelState state, VertexFormat format, Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter, CallbackInfoReturnable<IBakedModel> cir) {
        cir.setReturnValue(this.bakeImpl(state, format, bakedTextureGetter));
    }
}
