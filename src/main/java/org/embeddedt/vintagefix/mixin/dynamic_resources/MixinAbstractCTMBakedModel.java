package org.embeddedt.vintagefix.mixin.dynamic_resources;

import org.embeddedt.vintagefix.annotation.ClientOnlyMixin;
import org.embeddedt.vintagefix.annotation.LateMixin;
import org.embeddedt.vintagefix.dynamicresources.CTMHelper;
import org.spongepowered.asm.mixin.Mixin;
import team.chisel.ctm.client.model.AbstractCTMBakedModel;

@Mixin(value = AbstractCTMBakedModel.class, remap = false)
@LateMixin
@ClientOnlyMixin
public class MixinAbstractCTMBakedModel implements CTMHelper.AbstractCTMModelInterface {
}
