package org.embeddedt.vintagefix.mixin.dynamicresources.client;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.ResourceLocation;
import org.embeddedt.vintagefix.dynamicresources.IWeakTextureMap;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.Set;

@Mixin(TextureMap.class)
public abstract class MixinTextureMap implements IWeakTextureMap {

    @Shadow
    @Final
    private Map<String, TextureAtlasSprite> mapRegisteredSprites;

    @Shadow
    public abstract TextureAtlasSprite registerSprite(ResourceLocation location);

    private final Set<String> weakRegisteredSprites = new ObjectOpenHashSet<>();

    @Inject(method = "registerSprite", at = @At(value = "INVOKE", target = "Ljava/util/Map;get(Ljava/lang/Object;)Ljava/lang/Object;"))
    private void unregisterWeakSprite(ResourceLocation location, CallbackInfoReturnable<TextureAtlasSprite> cir) {
        String locKey = location.toString();
        if(this.weakRegisteredSprites.contains(locKey)) {
            this.mapRegisteredSprites.remove(locKey);
        }
    }

    @Inject(method = "setTextureEntry", at = @At("HEAD"), remap = false)
    private void unregisterWeakSprite2(TextureAtlasSprite sprite, CallbackInfoReturnable<Boolean> ci) {
        String key = sprite.getIconName();
        if(this.weakRegisteredSprites.contains(key)) {
            this.mapRegisteredSprites.remove(key);
        }
    }

    @Inject(method = "loadSprites", at = @At("HEAD"))
    private void clearWeakSprites(CallbackInfo ci) {
        this.weakRegisteredSprites.clear();
    }

    @Override
    public void registerSpriteWeak(ResourceLocation location) {
        this.registerSprite(location);
        this.weakRegisteredSprites.add(location.toString());
    }
}
