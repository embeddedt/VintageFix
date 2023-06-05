package org.embeddedt.vintagefix.mixin.model_optimizations.location_canon;

import org.embeddedt.vintagefix.annotation.ClientOnlyMixin;
import org.embeddedt.vintagefix.impl.Deduplicator;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModelResourceLocation.class)
@ClientOnlyMixin
public abstract class ModelResourceLocationMixin extends ResourceLocation {
    @Shadow
    @Final
    @Mutable
    private String variant;

    protected ModelResourceLocationMixin(int unused, String... resourceName) {
        super(unused, resourceName);
    }

    @Inject(method = "<init>(Lnet/minecraft/util/ResourceLocation;Ljava/lang/String;)V", at = @At("TAIL"))
    private void constructTail(ResourceLocation location, String variantIn, CallbackInfo ci) {
        // Do not use new strings for path and namespace, and deduplicate the variant string
        this.path = location.getPath();
        this.namespace = location.getNamespace();
        this.variant = Deduplicator.deduplicateVariant(this.variant);
    }
}
