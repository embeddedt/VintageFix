package org.embeddedt.vintagefix.mixin.dynamic_resources;

import net.minecraft.client.resources.AbstractResourcePack;
import org.embeddedt.vintagefix.annotation.ClientOnlyMixin;
import org.embeddedt.vintagefix.annotation.LateMixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

@Pseudo
@Mixin(targets = { "prospector/traverse/client/Traverse2Textures"})
@LateMixin
@ClientOnlyMixin
public abstract class Traverse2TexturesMixin extends AbstractResourcePack {
    @Shadow(remap = false)
    @Final
    private static Map<String, String> overrides;

    public Traverse2TexturesMixin(File resourcePackFileIn) {
        super(resourcePackFileIn);
    }

    @Inject(method = "func_110591_a", at = @At("HEAD"))
    private void checkExistenceOverride(String name, CallbackInfoReturnable<InputStream> cir) throws IOException {
        if(!overrides.containsKey(name))
            throw new FileNotFoundException(name);
    }
}
