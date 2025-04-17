package org.embeddedt.vintagefix.mixin.chunk_access;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.client.multiplayer.ChunkProviderClient;
import net.minecraft.world.chunk.Chunk;
import org.embeddedt.vintagefix.annotation.ClientOnlyMixin;
import org.embeddedt.vintagefix.chunk.VintageChunkMap;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ChunkProviderClient.class)
@ClientOnlyMixin
public class ChunkProviderClientMixin {
    @Shadow @Final private final Long2ObjectMap<Chunk> loadedChunks = new VintageChunkMap();
}
