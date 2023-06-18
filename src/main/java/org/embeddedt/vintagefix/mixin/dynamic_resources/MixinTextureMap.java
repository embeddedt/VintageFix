package org.embeddedt.vintagefix.mixin.dynamic_resources;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.client.renderer.StitcherException;
import net.minecraft.client.renderer.texture.Stitcher;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.ResourceLocation;
import org.embeddedt.vintagefix.ducks.IDroppingStitcher;
import org.embeddedt.vintagefix.dynamicresources.IWeakTextureMap;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
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

    @Inject(method = "loadTextureAtlas", at = @At("HEAD"))
    private void ignoreWeakTextures(CallbackInfo ci) {
        // at this point "weak"-ness no longer exists, it is only used to allow us to emulate vanilla during TextureStitchEvent.Pre
        // we need to treat registrations as strong for buggy mods like Binnie that try registering in Post
        this.weakRegisteredSprites.clear();
    }

    @Redirect(method = "finishLoading", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/texture/Stitcher;doStitch()V"), require = 0)
    private void tryStitchAndDropTexture(Stitcher stitcher) {
        if(!(stitcher instanceof IDroppingStitcher)) {
            stitcher.doStitch();
        } else {
            boolean stitchSuccess;
            while(true) {
                stitchSuccess = false;
                try {
                    stitcher.doStitch();
                    stitchSuccess = true;
                } catch(StitcherException ignored) {}
                if(stitchSuccess)
                    return;
                else {
                    /* drop largest sprite */
                    try {
                        ((IDroppingStitcher) stitcher).dropLargestSprite();
                    } catch(IllegalStateException e) {
                        throw new StitcherException(null, "Could not stitch even with all sprites removed");
                    }
                }
            }
        }
    }
}
