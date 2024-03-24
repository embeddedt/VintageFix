package org.embeddedt.vintagefix.mixin.allocation_rate;

import net.minecraft.util.math.BlockPos;
import net.minecraftforge.client.model.pipeline.VertexBufferConsumer;
import org.embeddedt.vintagefix.annotation.ClientOnlyMixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = VertexBufferConsumer.class, remap = false)
@ClientOnlyMixin
public interface AccessorVertexBufferConsumer {
    @Accessor("offset")
    void vintage$setOffset(BlockPos offset);
}
