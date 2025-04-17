package org.embeddedt.vintagefix.mixin.dynamic_resources;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.client.renderer.StitcherException;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.texture.Stitcher;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.fml.common.ProgressManager;
import org.embeddedt.vintagefix.VintageFix;
import org.embeddedt.vintagefix.annotation.ClientOnlyMixin;
import org.embeddedt.vintagefix.ducks.IDroppingStitcher;
import org.embeddedt.vintagefix.dynamicresources.IWeakTextureMap;
import org.embeddedt.vintagefix.dynamicresources.TextureCollector;
import org.embeddedt.vintagefix.dynamicresources.model.DynamicModelProvider;
import org.embeddedt.vintagefix.dynamicresources.model.ModelLocationInformation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;

@Mixin(TextureMap.class)
@ClientOnlyMixin
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
            this.weakRegisteredSprites.remove(locKey);
        }
    }

    @Inject(method = "setTextureEntry", at = @At("HEAD"), remap = false)
    private void unregisterWeakSprite2(TextureAtlasSprite sprite, CallbackInfoReturnable<Boolean> ci) {
        String key = sprite.getIconName();
        if(this.weakRegisteredSprites.contains(key)) {
            this.mapRegisteredSprites.remove(key);
            this.weakRegisteredSprites.remove(key);
        }
    }

    @Inject(method = "loadSprites", at = @At("HEAD"))
    private void clearWeakSprites(CallbackInfo ci) {
        this.weakRegisteredSprites.clear();
    }

    @Override
    public void registerSpriteWeak(ResourceLocation location) {
        String key = location.toString();
        if(this.weakRegisteredSprites.contains(key) || this.mapRegisteredSprites.containsKey(key))
            return;
        this.registerSprite(location);
        this.weakRegisteredSprites.add(key);
    }

    @Inject(method = "loadTextureAtlas", at = @At("HEAD"))
    private void ignoreWeakTextures(CallbackInfo ci) {
        // at this point "weak"-ness no longer exists, it is only used to allow us to emulate vanilla during TextureStitchEvent.Pre
        // we need to treat registrations as strong for buggy mods like Binnie that try registering in Post
        TextureCollector.weaklyCollectedTextures = new ObjectOpenHashSet<>(this.weakRegisteredSprites);
        this.weakRegisteredSprites.clear();
    }

    @Redirect(method = "finishLoading", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/texture/Stitcher;doStitch()V"), require = 0)
    private void tryStitchAndDropTexture(Stitcher stitcher) {
        if(!(stitcher instanceof IDroppingStitcher)) {
            stitcher.doStitch();
        } else {
            boolean stitchSuccess;
            boolean haveTriedFallbackPass = false;
            while(true) {
                stitchSuccess = false;
                try {
                    if(!haveTriedFallbackPass && Boolean.getBoolean("vintagefix.testFallbackPass"))
                        throw new StitcherException(null, "forced fallback");
                    stitcher.doStitch();
                    stitchSuccess = true;
                } catch(StitcherException ignored) {}
                if(stitchSuccess)
                    return;
                else {
                    if(!haveTriedFallbackPass) {
                        VintageFix.LOGGER.warn("Failed to fit all textures using greedy approach! Will try slow, scan-all-models fallback now...");
                        Set<ResourceLocation> allTextures = new HashSet<>();
                        // capture textures into this set
                        DynamicModelProvider.textureCapturer = allTextures;
                        DynamicModelProvider.instance.clearCache();
                        ProgressManager.ProgressBar bar = ProgressManager.push("Fallback texture gathering", 1);
                        /* try to load all models */
                        Set<ResourceLocation> loaded = new HashSet<>();
                        ConcurrentLinkedQueue<ResourceLocation> toLoad = new ConcurrentLinkedQueue<>(ModelLocationInformation.allKnownModelLocations);
                        ResourceLocation nextLoad;
                        while((nextLoad = toLoad.poll()) != null) {
                            if(loaded.contains(nextLoad))
                                continue;
                            loaded.add(nextLoad);
                            try {
                                IModel theModel = DynamicModelProvider.instance.getObject(nextLoad);
                                if(theModel != null) {
                                    for(ResourceLocation dep : theModel.getDependencies()) {
                                        if(!loaded.contains(dep))
                                            toLoad.add(dep);
                                    }
                                }
                            } catch(Exception ignored) {
                            }
                        }
                        bar.step("");
                        ProgressManager.pop(bar);
                        // stop capturing
                        DynamicModelProvider.textureCapturer = null;
                        haveTriedFallbackPass = true;
                        // drop all other textures
                        ((IDroppingStitcher)stitcher).retainAllSprites(allTextures);
                    } else {
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
}
