package org.embeddedt.vintagefix.mixin.allocation_rate;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.profiler.Profiler;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
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

    @Shadow
    @Final
    private Minecraft mc;

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

    private int vfix$lastSectionX = Integer.MIN_VALUE, vfix$lastSectionZ = Integer.MIN_VALUE, vfix$lastRenderDistance = 0;

    /**
     * @author embeddedt
     * @reason only repopulate this set when player moves into a new chunk section
     */
    @Inject(method = "refreshVisibleChunks", at = @At("HEAD"), cancellable = true)
    private void skipIfSectionUnchanged(CallbackInfo ci) {
        int sx = MathHelper.floor(this.mc.player.posX / 16.0D);
        int sz = MathHelper.floor(this.mc.player.posZ / 16.0D);

        if(sx == vfix$lastSectionX && sz == vfix$lastSectionZ && this.mc.gameSettings.renderDistanceChunks == vfix$lastRenderDistance) {
            ci.cancel();
            return;
        }

        vfix$lastSectionX = sx;
        vfix$lastSectionZ = sz;
        vfix$lastRenderDistance = this.mc.gameSettings.renderDistanceChunks;
    }
}
