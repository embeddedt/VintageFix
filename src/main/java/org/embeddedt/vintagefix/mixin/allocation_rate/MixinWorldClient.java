package org.embeddedt.vintagefix.mixin.allocation_rate;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.profiler.Profiler;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.WorldSettings;
import org.embeddedt.vintagefix.annotation.ClientOnlyMixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(WorldClient.class)
@ClientOnlyMixin
public class MixinWorldClient {
    @Shadow
    protected Set<ChunkPos> visibleChunks;

    @Shadow
    @Mutable
    @Final
    private Set<ChunkPos> previousActiveChunkSet;

    /**
     * @author embeddedt
     * @reason Replace these sets with faster and less allocation-heavy alternatives. This is done once at world creation,
     * so it should be safe.
     */
    @Inject(method = "<init>", at = @At("RETURN"))
    private void replaceChunkSets(NetHandlerPlayClient netHandler, WorldSettings settings, int dimension, EnumDifficulty difficulty, Profiler profilerIn, CallbackInfo ci) {
        this.visibleChunks = new ObjectOpenHashSet<>(this.visibleChunks);
        this.previousActiveChunkSet = new ObjectOpenHashSet<>(this.previousActiveChunkSet);
    }
}
