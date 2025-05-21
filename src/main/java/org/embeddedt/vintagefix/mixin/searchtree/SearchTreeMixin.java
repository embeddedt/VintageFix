package org.embeddedt.vintagefix.mixin.searchtree;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import net.minecraft.client.util.SearchTree;
import org.embeddedt.vintagefix.VintageFix;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(SearchTree.class)
public abstract class SearchTreeMixin<T> {
    private static final ThreadLocal<Boolean> DEFER_RECALCULATION = ThreadLocal.withInitial(() -> true);
    private volatile boolean vfix$needsRecalculation;

    @Shadow
    public abstract void recalculate();

    @Shadow @Final
    private List<T> contents;


    @Inject(method = "recalculate()V", at = @At("HEAD"), cancellable = true)
    private void deferRecalculation(CallbackInfo ci) {
        if (DEFER_RECALCULATION.get()) {
            vfix$needsRecalculation = true;
            ci.cancel();
        }
    }

    /**
     * @author embeddedt
     * @reason We do not need to index at this stage as the index maps are recomputed when recalculate is called anyway.
     */
    @Redirect(method = "add(Ljava/lang/Object;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/SearchTree;index(Ljava/lang/Object;)V"))
    private void skipIndex(SearchTree<T> tree, Object o) {

    }

    @Inject(method = "search(Ljava/lang/String;)Ljava/util/List;", at = @At("HEAD"))
    private void recalculateBeforeSearch(CallbackInfoReturnable<List<T>> cir) {
        if (vfix$needsRecalculation) {
            synchronized (this) {
                if (vfix$needsRecalculation) {
                    DEFER_RECALCULATION.set(false);
                    VintageFix.LOGGER.info("Building search tree for {} items (this may take a while)...", contents.size());
                    Stopwatch watch = Stopwatch.createStarted();
                    try {
                        this.recalculate();
                    } finally {
                        DEFER_RECALCULATION.set(true);
                    }
                    watch.stop();
                    VintageFix.LOGGER.info("Building search tree for {} items took {}", contents.size(), watch);
                    vfix$needsRecalculation = false;
                }
            }
        }
    }
}
