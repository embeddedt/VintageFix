package org.embeddedt.vintagefix.dynamicresources;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;

public interface IBlockModelShapes {
    ModelResourceLocation getLocationForState(IBlockState state);
}
