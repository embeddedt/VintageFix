package org.embeddedt.vintagefix.mixin.model_optimizations.dedup_quad_lists;

import org.embeddedt.vintagefix.annotation.ClientOnlyMixin;
import org.embeddedt.vintagefix.impl.ModelSidesImpl;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.SimpleBakedModel;
import net.minecraft.util.EnumFacing;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;

@Mixin(SimpleBakedModel.class)
@ClientOnlyMixin
public class SimpleBakedModelMixin {
    @Shadow
    @Final
    @Mutable
    protected Map<EnumFacing, List<BakedQuad>> faceQuads;

    @Shadow
    @Final
    @Mutable
    protected List<BakedQuad> generalQuads;

    // Target all constructors, Forge adds an argument to the "main" constructor
    @Inject(method = "<init>", at = @At("TAIL"))
    private void minimizeFaceLists(CallbackInfo ci) {
        this.generalQuads = ModelSidesImpl.minimizeUnculled(this.generalQuads);
        this.faceQuads = ModelSidesImpl.minimizeCulled(this.faceQuads);
    }
}
