package org.embeddedt.vintagefix.dynamicresources;

import net.minecraft.client.renderer.block.model.IBakedModel;

public interface IModelHoldingBlockState {
    IBakedModel vfix$getModel();
    void vfix$setModel(IBakedModel model);
}
