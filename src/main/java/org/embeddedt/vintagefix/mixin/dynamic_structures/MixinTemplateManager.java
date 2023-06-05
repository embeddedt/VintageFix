package org.embeddedt.vintagefix.mixin.dynamic_structures;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.minecraft.world.gen.structure.template.Template;
import net.minecraft.world.gen.structure.template.TemplateManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(TemplateManager.class)
public class MixinTemplateManager {
    @Shadow
    @Final
    @Mutable
    private Map<String, Template> templates;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void useDynamicMap(CallbackInfo ci) {
        /* Structures needing to be reloaded is not a huge issue since we optimize loading them already */
        Cache<String, Template> structureCache = CacheBuilder.newBuilder()
            .softValues()
            .build();
        this.templates = structureCache.asMap();
    }
}
