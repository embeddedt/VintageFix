package org.embeddedt.vintagefix.mixin.model_optimizations.simpler_perspective_wrapper;

import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraftforge.client.model.PerspectiveMapWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PerspectiveMapWrapper.class)
public interface AccessorPerspectiveModelWrapper {
    @Accessor(value = "parent", remap = false)
    IBakedModel getParent();
}
