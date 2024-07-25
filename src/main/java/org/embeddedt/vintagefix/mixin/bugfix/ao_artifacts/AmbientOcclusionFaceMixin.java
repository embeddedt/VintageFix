package org.embeddedt.vintagefix.mixin.bugfix.ao_artifacts;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import org.embeddedt.vintagefix.annotation.ClientOnlyMixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;

@Mixin(targets = {"net/minecraft/client/renderer/BlockModelRenderer$AmbientOcclusionFace"})
@ClientOnlyMixin
public class AmbientOcclusionFaceMixin {
    /**
     * @author embeddedt
     * @reason Fix MC-43968 by using the correct positions for determining whether neighboring blocks are opaque
     */
    @Redirect(
        method = "updateVertexBrightness",
        slice = @Slice(
            from = @At(value = "INVOKE", target = "Lnet/minecraft/block/state/IBlockState;getAmbientOcclusionLightValue()F", ordinal = 3),
            to = @At(value = "INVOKE", target = "Lnet/minecraft/block/state/IBlockState;isTranslucent()Z", ordinal = 3)
        ),
        at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/BlockPos$PooledMutableBlockPos;move(Lnet/minecraft/util/EnumFacing;)Lnet/minecraft/util/math/BlockPos$PooledMutableBlockPos;"),
        require = 4,
        allow = 4
    )
    private BlockPos.PooledMutableBlockPos skipMoving(BlockPos.PooledMutableBlockPos pos, EnumFacing direction) {
        return pos;
    }
}
