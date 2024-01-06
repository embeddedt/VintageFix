package org.embeddedt.vintagefix.mixin.allocation_rate;

import net.minecraft.util.math.BlockPos;
import net.minecraftforge.client.model.pipeline.BlockInfo;
import org.embeddedt.vintagefix.annotation.ClientOnlyMixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BlockInfo.class)
@ClientOnlyMixin
public class MixinBlockInfo {
    private final BlockPos.MutableBlockPos vfix$cursor = new BlockPos.MutableBlockPos();

    @Redirect(method = "updateLightMatrix", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/BlockPos;add(III)Lnet/minecraft/util/math/BlockPos;", ordinal = 0))
    private BlockPos useMutableCursor(BlockPos pos, int x, int y, int z) {
        return vfix$cursor.setPos(pos.getX() + x, pos.getY() + y, pos.getZ() + z);
    }
}
