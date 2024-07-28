package org.embeddedt.vintagefix.mixin.chunk_rendering;

import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.util.math.BlockPos;
import org.embeddedt.vintagefix.annotation.ClientOnlyMixin;
import org.embeddedt.vintagefix.util.SectionBlockPosIterator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(RenderChunk.class)
@ClientOnlyMixin
public class MixinRenderChunk {
    /**
     * @author embeddedt
     * @reason replace complex iterator with simpler one
     */
    @Redirect(method = "rebuildChunk", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/BlockPos;getAllInBoxMutable(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/math/BlockPos;)Ljava/lang/Iterable;"))
    private Iterable<BlockPos> getIterator(BlockPos base, BlockPos max) {
        return () -> new SectionBlockPosIterator(base);
    }
}
