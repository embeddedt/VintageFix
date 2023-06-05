package org.embeddedt.vintagefix.mixin.mod_opts.agricraft;

import com.google.common.base.Predicate;
import org.embeddedt.vintagefix.annotation.LateMixin;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Pseudo
@Mixin(targets = "com/agricraft/agricore/util/ResourceHelper")
@LateMixin
public class ResourceHelperMixin {
    @Shadow(remap = false)
    @Final
    @Mutable
    private static Reflections REFLECTIONS;

    @Inject(method = "<clinit>", at = @At("RETURN"))
    private static void destroyStaticReflections(CallbackInfo ci) {
        REFLECTIONS = null;
    }

    @Redirect(method = "findResources", at = @At(value = "INVOKE", target = "Lorg/reflections/Reflections;getResources(Lcom/google/common/base/Predicate;)Ljava/util/Set;"), remap = false)
    private static Set<String> getAllResources(Reflections r, Predicate<String> predicate) {
        return new Reflections(null, new ResourcesScanner()).getResources(predicate);
    }
}
