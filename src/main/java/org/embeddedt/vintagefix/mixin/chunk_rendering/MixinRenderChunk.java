package org.embeddedt.vintagefix.mixin.chunk_rendering;

import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.client.ForgeHooksClient;
import org.embeddedt.vintagefix.annotation.ClientOnlyMixin;
import org.embeddedt.vintagefix.util.SectionBlockPosIterator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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

    /**
     * @author embeddedt
     * @reason reduce number of calls to thread local
     */
    @Redirect(method = "rebuildChunk", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/client/ForgeHooksClient;setRenderLayer(Lnet/minecraft/util/BlockRenderLayer;)V", ordinal = 1))
    private void skipNullingLayer(BlockRenderLayer layer) {
        if(layer != null) throw new AssertionError();
    }

    /**
     * @author embeddedt
     * @reason reset layer
     */
    @Inject(method = "rebuildChunk", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/chunk/CompiledChunk;setVisibility(Lnet/minecraft/client/renderer/chunk/SetVisibility;)V"))
    private void resetLayer(CallbackInfo ci) {
        ForgeHooksClient.setRenderLayer(null);
    }
}
