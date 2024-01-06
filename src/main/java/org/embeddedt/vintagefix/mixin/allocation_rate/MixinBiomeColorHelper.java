package org.embeddedt.vintagefix.mixin.allocation_rate;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.biome.BiomeColorHelper;
import org.embeddedt.vintagefix.annotation.ClientOnlyMixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(BiomeColorHelper.class)
@ClientOnlyMixin
public class MixinBiomeColorHelper {
    /**
     * @author embeddedt
     * @reason reduce object allocation spam
     */
    @Overwrite
    private static int getColorAtPos(IBlockAccess blockAccess, BlockPos pos, BiomeColorHelper.ColorResolver colorResolver)
    {
        int i = 0;
        int j = 0;
        int k = 0;

        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        for(int z = -1; z <= 1; z++) {
            for(int x = -1; x <= 1; x++) {
                mutablePos.setPos(pos.getX() + x, pos.getY(), pos.getZ() + z);
                int l = colorResolver.getColorAtPos(blockAccess.getBiome(mutablePos), mutablePos);
                i += (l & 16711680) >> 16;
                j += (l & 65280) >> 8;
                k += l & 255;
            }
        }

        return (i / 9 & 255) << 16 | (j / 9 & 255) << 8 | k / 9 & 255;
    }
}
