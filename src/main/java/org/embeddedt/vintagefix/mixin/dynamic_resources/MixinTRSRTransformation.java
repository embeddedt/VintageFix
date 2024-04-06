package org.embeddedt.vintagefix.mixin.dynamic_resources;

import net.minecraftforge.common.model.TRSRTransformation;
import org.embeddedt.vintagefix.annotation.ClientOnlyMixin;
import org.embeddedt.vintagefix.util.MatrixHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.vecmath.Matrix4f;

@Mixin(value = TRSRTransformation.class, remap = false)
@ClientOnlyMixin
public class MixinTRSRTransformation {
    @Shadow
    @Final
    private Matrix4f matrix;
    private boolean vfix$isIdentity;


    @Inject(method = "<init>*", at = @At("RETURN"))
    private void cacheIdentity(CallbackInfo ci) {
        this.vfix$isIdentity = this.matrix.equals(MatrixHelper.IDENTITY_4X4);
    }

    /**
     * @author embeddedt
     * @reason do not waste time comparing matrices
     */
    @Overwrite
    public boolean isIdentity() {
        return this.vfix$isIdentity;
    }
}
