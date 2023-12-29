package org.embeddedt.vintagefix.mixin.chunk_access;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChunkProviderServer;
import org.embeddedt.vintagefix.chunk.VintageChunkMap;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ChunkProviderServer.class)
public class ChunkProviderServerMixin {
    @Shadow
    @Final
    private final Long2ObjectMap<Chunk> loadedChunks = new VintageChunkMap();
}
