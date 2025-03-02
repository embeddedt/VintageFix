package org.embeddedt.vintagefix.dynamicresources;

import net.minecraft.client.renderer.block.model.ModelBlockDefinition;
import net.minecraft.util.ResourceLocation;

public interface IExtendedModelLoader {
    ModelBlockDefinition vfix$getModelBlockDef(ResourceLocation location);
}
