package org.embeddedt.vintagefix.mixin.model_optimizations.dedup_multipart;

import com.google.common.base.Predicate;
import org.embeddedt.vintagefix.annotation.ClientOnlyMixin;
import org.embeddedt.vintagefix.impl.Deduplicator;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.MultipartBakedModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Map;

@Mixin(MultipartBakedModel.Builder.class)
@ClientOnlyMixin
public class MixinMultipartBuilder {
    @Redirect(
            method = "makeMultipartModel",
            at = @At(value = "NEW", target = "net/minecraft/client/renderer/block/model/MultipartBakedModel")
    )
    public MultipartBakedModel build(Map<Predicate<IBlockState>, IBakedModel> selectors) {
        return Deduplicator.makeMultipartModel(selectors);
    }
}
