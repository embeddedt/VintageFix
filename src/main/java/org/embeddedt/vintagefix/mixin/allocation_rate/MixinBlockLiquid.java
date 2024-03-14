package org.embeddedt.vintagefix.mixin.allocation_rate;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import org.embeddedt.vintagefix.annotation.ClientOnlyMixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BlockLiquid.class)
@ClientOnlyMixin
public abstract class MixinBlockLiquid extends Block {
    private static final ThreadLocal<BlockPos.MutableBlockPos> VFIX$CURSOR = ThreadLocal.withInitial(BlockPos.MutableBlockPos::new);

    public MixinBlockLiquid(Material materialIn) {
        super(materialIn);
    }

    /**
     * @author embeddedt
     * @reason avoid BlockPos allocation
     */
    @Redirect(method = "shouldSideBeRendered", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/BlockPos;offset(Lnet/minecraft/util/EnumFacing;)Lnet/minecraft/util/math/BlockPos;"), require = 0)
    public BlockPos shouldSideBeRendered(BlockPos pos, EnumFacing side)
    {
        BlockPos.MutableBlockPos cursor = VFIX$CURSOR.get();
        cursor.setPos(pos.getX() + side.getXOffset(), pos.getY() + side.getYOffset(), pos.getZ() + side.getZOffset());
        return cursor;
    }

    /**
     * @author embeddedt
     * @reason avoid BlockPos allocation
     */
    @Redirect(method = "getPackedLightmapCoords", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/BlockPos;up()Lnet/minecraft/util/math/BlockPos;"), require = 0)
    private BlockPos useCursorForUp(BlockPos instance) {
        BlockPos.MutableBlockPos cursor = VFIX$CURSOR.get();
        cursor.setPos(instance.getX(), instance.getY() + 1, instance.getZ());
        return cursor;
    }
}
