package org.embeddedt.vintagefix.mixin.dynamic_resources;

import com.google.common.collect.ImmutableSet;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.client.resources.SimpleReloadableResourceManager;
import org.embeddedt.vintagefix.annotation.ClientOnlyMixin;
import org.embeddedt.vintagefix.dynamicresources.DeferredListeners;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(SimpleReloadableResourceManager.class)
@ClientOnlyMixin
public class MixinSimpleReloadableResourceManager {
    private static final Set<String> DEFERRED_LISTENER_CLASSES = ImmutableSet.<String>builder()
        .add("slimeknights.tconstruct.library.client.CustomTextureCreator")
        .build();
    @Inject(method = "registerReloadListener", at = @At("HEAD"), cancellable = true)
    private void registerDeferredListener(IResourceManagerReloadListener listener, CallbackInfo ci) {
        if(DEFERRED_LISTENER_CLASSES.contains(listener.getClass().getName())) {
            DeferredListeners.deferredListeners.add(listener);
            ci.cancel();
        }
    }
}
