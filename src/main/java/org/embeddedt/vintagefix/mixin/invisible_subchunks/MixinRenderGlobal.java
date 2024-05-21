package org.embeddedt.vintagefix.mixin.invisible_subchunks;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.chunk.CompiledChunk;
import net.minecraft.util.EnumFacing;
import org.embeddedt.vintagefix.ducks.IPathingSetVisibility;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;

@Mixin(RenderGlobal.class)
public class MixinRenderGlobal {
    /**
     * @author MrGrim
     * @reason Prevent excessive removal of neighboring subchunks (MC-63020)
     */
    @ModifyExpressionValue(method = "setupTerrain", at = @At(value = "INVOKE", target = "Ljava/util/Set;size()I", remap = false, ordinal = 0),
        slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderGlobal;getVisibleFacings(Lnet/minecraft/util/math/BlockPos;)Ljava/util/Set;", ordinal = 0),
            to = @At(value = "INVOKE", target = "Lnet/minecraft/util/EnumFacing;getFacingFromVector(FFF)Lnet/minecraft/util/EnumFacing;", ordinal = 0)))
    private int overrideOpposingSideCheck(int prevSize) {
        return 0;
    }

    @Redirect(method = "setupTerrain", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/chunk/CompiledChunk;isVisible(Lnet/minecraft/util/EnumFacing;Lnet/minecraft/util/EnumFacing;)Z", ordinal = 0),
        slice = @Slice(from = @At(value = "CONSTANT", args="stringValue=iteration"), to = @At(value = "CONSTANT", args="stringValue=captureFrustum")))
    private boolean checkTargetVisibility(CompiledChunk chunkIn, EnumFacing fromFace, EnumFacing toFace)
    {
        return ((IPathingSetVisibility)((AccessorCompiledChunk) chunkIn).vfix$getSetVisibility()).vfix$anyPathToFace(toFace);
    }

    // TODO: doesn't seem to be necessary?
    /*
    @ModifyArg(method = "setupTerrain", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderGlobal$ContainerLocalRenderInformation;setDirection(BLnet/minecraft/util/EnumFacing;)V", ordinal = 0), index = 0,
        slice = @Slice(from = @At(value = "CONSTANT", args="stringValue=iteration"), to = @At(value = "CONSTANT", args="stringValue=captureFrustum")))
    private byte disableStickyReversePath(byte setFacingIn)
    {
        return (byte) 0;
    }
    */
}
