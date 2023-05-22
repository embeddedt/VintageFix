package org.embeddedt.vintagefix.mixin.dynamicresources.client;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.BlockModelShapes;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import org.embeddedt.vintagefix.dynamicresources.IBlockModelShapes;
import org.embeddedt.vintagefix.dynamicresources.model.DynamicBakedModelProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import team.chisel.ctm.client.asm.CTMCoreMethods;

@Mixin(CTMCoreMethods.class)
public class MixinCTMCoreMethods {
    @Redirect(method = "canRenderInLayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/BlockRendererDispatcher;getModelForState(Lnet/minecraft/block/state/IBlockState;)Lnet/minecraft/client/renderer/block/model/IBakedModel;"))
    private static IBakedModel getModelForState(BlockRendererDispatcher dispatcher, IBlockState state) {
        // Only get the model if its actually available at this time, to avoid yanking in tons of models
        // FIXME may cause weird issues with layers on CTM blocks
        BlockModelShapes shapes = dispatcher.getBlockModelShapes();
        ModelResourceLocation mrl = ((IBlockModelShapes)shapes).getLocationForState(state);
        IBakedModel m = mrl != null ? DynamicBakedModelProvider.instance.getModelIfPresent(mrl) : null;
        if(m != null)
            return m;
        else
            return shapes.getModelManager().getMissingModel();
    }
}
