package org.embeddedt.vintagefix.mixin.invisible_subchunks;

import net.minecraft.client.renderer.chunk.CompiledChunk;
import net.minecraft.client.renderer.chunk.SetVisibility;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(CompiledChunk.class)
public interface AccessorCompiledChunk {
    @Accessor("setVisibility")
    SetVisibility vfix$getSetVisibility();
}
