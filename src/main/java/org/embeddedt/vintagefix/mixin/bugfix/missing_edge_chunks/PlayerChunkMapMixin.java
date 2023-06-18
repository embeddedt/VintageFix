package org.embeddedt.vintagefix.mixin.bugfix.missing_edge_chunks;

import net.minecraft.server.management.PlayerChunkMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(PlayerChunkMap.class)
public class PlayerChunkMapMixin {
    /**
     * 1.12 doesn't send edge chunks to the client if they are not post-processed and/or entity ticking. To work around
     * this without more invasive technical changes we simply load an extra border of 2 chunks here in order to make sure
     * the fog will always look right. The global server view distance is not changed, only the handling within the
     * player chunk map itself.
     *
     * It's not ideal, but since the chunks are not technically within the view distance, they should just be loaded
     * and therefore the performance impact is hopefully minimal.
     */
    @ModifyVariable(method = "setPlayerViewRadius", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private int boostRadius(int original) {
        return original + 2;
    }
}
