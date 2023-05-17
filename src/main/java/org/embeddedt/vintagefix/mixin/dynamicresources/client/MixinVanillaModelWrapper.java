package org.embeddedt.vintagefix.mixin.dynamicresources.client;

import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.common.model.IModelState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.function.Function;

@Mixin(targets = "net/minecraftforge/client/model/ModelLoader$VanillaModelWrapper")
public abstract class MixinVanillaModelWrapper {
    @Shadow
    public abstract IBakedModel bakeImpl(IModelState state, final VertexFormat format, Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter);

    @Redirect(method = "getDependencies", at = @At(value = "INVOKE", target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"), remap = false)
    private Object skipPutStateModel(Map map, Object k, Object v) {
        return null;
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
