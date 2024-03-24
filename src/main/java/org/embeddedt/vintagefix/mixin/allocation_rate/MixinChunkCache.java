package org.embeddedt.vintagefix.mixin.allocation_rate;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ChunkCache;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import org.embeddedt.vintagefix.annotation.ClientOnlyMixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ChunkCache.class ,priority = 500)
@ClientOnlyMixin
public abstract class MixinChunkCache {
    @Shadow
    protected World world;

    @Shadow
    public abstract IBlockState getBlockState(BlockPos pos);

    @Shadow
    public abstract int getLightFor(EnumSkyBlock type, BlockPos pos);

    @Shadow
    protected int chunkX;

    @Shadow
    protected int chunkZ;

    @Shadow
    protected abstract boolean withinBounds(int x, int z);

    @Shadow
    protected Chunk[][] chunkArray;

    /**
     * @author embeddedt
     * @reason reduce BlockPos allocations when checking neighbor brightness
     */
    @Overwrite
    private int getLightForExt(EnumSkyBlock type, BlockPos pos) {
        if (type == EnumSkyBlock.SKY && !this.world.provider.hasSkyLight()) {
            return 0;
        } else if (pos.getY() >= 0 && pos.getY() < 256) {
            if (this.getBlockState(pos).useNeighborBrightness()) {
                // VintageFix change: use mutable BlockPos so we only allocate once
                BlockPos.MutableBlockPos offsetPos = new BlockPos.MutableBlockPos();
                int l = 0;

                for (EnumFacing enumfacing : EnumFacing.VALUES) {
                    offsetPos.setPos(pos.getX() + enumfacing.getXOffset(), pos.getY() + enumfacing.getYOffset(), pos.getZ() + enumfacing.getZOffset());
                    int k = this.getLightFor(type, offsetPos);

                    if (k > l) {
                        l = k;
                    }

                    if (l >= 15) {
                        return l;
                    }
                }

                return l;
            } else {
                int i = (pos.getX() >> 4) - this.chunkX;
                int j = (pos.getZ() >> 4) - this.chunkZ;
                if (!withinBounds(i, j)) return type.defaultLightValue;
                return this.chunkArray[i][j].getLightFor(type, pos);
            }
        } else {
            return type.defaultLightValue;
        }
    }
}
