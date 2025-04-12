package org.embeddedt.vintagefix.mixin.bugfix.exit_freeze;

import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.server.management.PlayerProfileCache;
import net.minecraft.util.datafix.DataFixer;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import org.embeddedt.vintagefix.VintageFix;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.io.File;
import java.net.Proxy;
import java.util.concurrent.*;

@Mixin(IntegratedServer.class)
public abstract class IntegratedServerMixin extends MinecraftServer {
    public IntegratedServerMixin(File anvilFileIn, Proxy proxyIn, DataFixer dataFixerIn, YggdrasilAuthenticationService authServiceIn, MinecraftSessionService sessionServiceIn, GameProfileRepository profileRepoIn, PlayerProfileCache profileCacheIn) {
        super(anvilFileIn, proxyIn, dataFixerIn, authServiceIn, sessionServiceIn, profileRepoIn, profileCacheIn);
    }

    /**
     * Sometimes the server thread is already gone at this point, try to prevent a freeze.
     */
    @Redirect(method = "initiateShutdown", at = @At(value = "INVOKE", target = "Lcom/google/common/util/concurrent/Futures;getUnchecked(Ljava/util/concurrent/Future;)Ljava/lang/Object;", remap = false), require = 0)
    private <V> V checkThreadStatus(Future<V> future) {
        while(this.isServerRunning() && ((Thread)ObfuscationReflectionHelper.getPrivateValue(MinecraftServer.class, this, "field_175590_aa")).isAlive()) {
            try {
                return future.get(500, TimeUnit.MILLISECONDS);
            } catch(InterruptedException e) {
                break;
            } catch(ExecutionException | CancellationException e) {
                throw new RuntimeException(e);
            } catch(TimeoutException ignored) {}
        }
        VintageFix.LOGGER.warn("Server thread has already exited, skipping logout process");
        return null;
    }
}
