package org.embeddedt.vintagefix.mixin.allocation_rate;

import net.minecraft.util.math.BlockPos;
import net.minecraftforge.client.model.pipeline.ForgeBlockModelRenderer;
import net.minecraftforge.client.model.pipeline.VertexBufferConsumer;
import org.embeddedt.vintagefix.annotation.ClientOnlyMixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ForgeBlockModelRenderer.class)
@ClientOnlyMixin
public class MixinForgeBlockModelRenderer {
    @Redirect(method = "*", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/client/model/pipeline/VertexBufferConsumer;setOffset(Lnet/minecraft/util/math/BlockPos;)V"), require = 0)
    private void setOffsetWithoutAlloc(VertexBufferConsumer consumer, BlockPos offset) {
        ((AccessorVertexBufferConsumer)consumer).vintage$setOffset(offset);
    }
}
