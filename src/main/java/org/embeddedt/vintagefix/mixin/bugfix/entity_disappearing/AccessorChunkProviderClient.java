package org.embeddedt.vintagefix.mixin.bugfix.entity_disappearing;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.client.multiplayer.ChunkProviderClient;
import net.minecraft.world.chunk.Chunk;
import org.embeddedt.vintagefix.annotation.ClientOnlyMixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ChunkProviderClient.class)
@ClientOnlyMixin
public interface AccessorChunkProviderClient {
    @Accessor("loadedChunks")
    Long2ObjectMap<Chunk> vfix$getLoadedChunks();
}
