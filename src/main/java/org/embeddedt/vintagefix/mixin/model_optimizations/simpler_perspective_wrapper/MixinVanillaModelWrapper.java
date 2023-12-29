package org.embeddedt.vintagefix.mixin.model_optimizations.simpler_perspective_wrapper;

import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelBlock;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.PerspectiveMapWrapper;
import net.minecraftforge.client.model.animation.ModelBlockAnimation;
import net.minecraftforge.common.model.IModelState;
import net.minecraftforge.common.model.TRSRTransformation;
import org.embeddedt.vintagefix.annotation.ClientOnlyMixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.function.Function;

@Mixin(targets = "net/minecraftforge/client/model/ModelLoader$VanillaModelWrapper")
@ClientOnlyMixin
public class MixinVanillaModelWrapper {
    @Shadow(remap = false)
    @Final
    private ModelBlockAnimation animation;

    /**
     * Avoid storing a PerspectiveModelWrapper anonymous class with special handling for animations if
     * there is no model animation. Ported from FoamFix.
     */
    @Inject(method = "bakeNormal", at = @At("RETURN"), remap = false, cancellable = true)
    private void useRegularPerspectiveWrapper(ModelBlock model, IModelState perState, final IModelState modelState, List<TRSRTransformation> newTransforms, final VertexFormat format, final Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter, boolean uvLocked, CallbackInfoReturnable<IBakedModel> cir) {
        if(cir.getReturnValue() instanceof PerspectiveMapWrapper
            && cir.getReturnValue().getClass() != PerspectiveMapWrapper.class
            && (animation == null || animation.getClips().isEmpty())) {
            // reconstruct a simpler, smaller wrapper object
            cir.setReturnValue(new PerspectiveMapWrapper(((AccessorPerspectiveModelWrapper)cir.getReturnValue()).getParent(), perState));
        }
    }
}
