package org.embeddedt.vintagefix.mixin.chunk_rendering;

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import mrtjp.projectred.relocation.MovementManager$;
import mrtjp.projectred.relocation.MovingBlockRenderDispatcher;
import mrtjp.projectred.relocation.WorldStructs;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.embeddedt.vintagefix.annotation.ClientOnlyMixin;
import org.embeddedt.vintagefix.annotation.LateMixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import scala.Option;
import scala.collection.mutable.HashMap;

@Mixin(MovingBlockRenderDispatcher.class)
@ClientOnlyMixin
@LateMixin
public class MixinMovingBlockRenderDispatcher {
    /**
     * @author embeddedt
     * @reason Improve performance when nothing is being moved
     */
    @Redirect(method = "renderBlock", at = @At(value = "INVOKE", target = "Lmrtjp/projectred/relocation/MovementManager$;isMoving(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;)Z"))
    private boolean fasterMovingCheck(MovementManager$ instance, World world, BlockPos blockPos, @Share("worldStructs") LocalRef<WorldStructs> ref) {
        HashMap<Object, WorldStructs> map = MovementManager$.MODULE$.clientRelocations();
        if(!map.isEmpty()) {
            Option<WorldStructs> structs = map.get(world.provider.getDimension());
            if(structs.isDefined()) {
                WorldStructs wStructs = structs.get();
                if(!wStructs.isEmpty()) {
                    ref.set(wStructs);
                    return wStructs.contains(blockPos);
                }
            }
        }
        return false;
    }

    /**
     * @author embeddedt
     * @reason Improve performance when nothing is being moved
     */
    @Redirect(method = "renderBlock", at = @At(value = "INVOKE", target = "Lmrtjp/projectred/relocation/MovementManager$;isAdjacentToMoving(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;)Z"))
    private boolean fasterAdjacentCheck(MovementManager$ instance, World world, BlockPos blockPos, @Share("worldStructs") LocalRef<WorldStructs> ref) {
        WorldStructs structs = ref.get();
        return structs != null && structs.isAdjacentTo(blockPos);
    }
}
