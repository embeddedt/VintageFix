package org.embeddedt.vintagefix.mixin.chunk_access;

import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = Chunk.class, priority = 500)
public class ChunkMixin {
    @Shadow
    @Final
    private ExtendedBlockStorage[] storageArrays;

    @Shadow
    @Final
    public static ExtendedBlockStorage NULL_BLOCK_STORAGE;

    private static final IBlockState DEFAULT_BLOCK_STATE = Blocks.AIR.getDefaultState();

    /**
     * @reason Reduce method size to help the JVM inline
     * @author JellySquid
     */
    @Overwrite
    public IBlockState getBlockState(int x, int y, int z) {
        if (y >= 0 && (y >> 4) < this.storageArrays.length) {
            ExtendedBlockStorage section = this.storageArrays[y >> 4];

            if (section != NULL_BLOCK_STORAGE) {
                return section.get(x & 15, y & 15, z & 15);
            }
        }

        return DEFAULT_BLOCK_STATE;
    }
}
