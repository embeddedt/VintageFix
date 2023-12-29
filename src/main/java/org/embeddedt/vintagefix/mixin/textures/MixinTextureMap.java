package org.embeddedt.vintagefix.mixin.textures;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.client.renderer.texture.PngSizeInfo;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.data.IMetadataSection;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.ProgressManager;
import org.apache.logging.log4j.Logger;
import org.embeddedt.vintagefix.VintageFix;
import org.embeddedt.vintagefix.annotation.ClientOnlyMixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

@Mixin(TextureMap.class)
@ClientOnlyMixin
public abstract class MixinTextureMap {
    @Shadow
    protected abstract ResourceLocation getResourceLocation(TextureAtlasSprite p_184396_1_);

    @Shadow
    @Final
    private Map<String, TextureAtlasSprite> mapRegisteredSprites;
    @Shadow
    @Final
    private String basePath;
    @Shadow
    private int mipmapLevels;
    private static final String TEXTURE_LOADER_CORE = "loadTexture(Lnet/minecraft/client/renderer/texture/Stitcher;Lnet/minecraft/client/resources/IResourceManager;Lnet/minecraft/util/ResourceLocation;Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;Lnet/minecraftforge/fml/common/ProgressManager$ProgressBar;II)I";
    private static final String TEXTURE_LOADER_CORE_OF = "loadTextureAtlas(Lnet/minecraft/client/resources/IResourceManager;)V";

    private static final IResource EMPTY_META_RESOURCE = new IResource() {
        @Override
        public ResourceLocation getResourceLocation() {
            return new ResourceLocation("stub");
        }

        @Override
        public InputStream getInputStream() {
            return null;
        }

        @Override
        public boolean hasMetadata() {
            return false;
        }

        @Nullable
        @Override
        public <T extends IMetadataSection> T getMetadata(String sectionName) {
            return null;
        }

        @Override
        public String getResourcePackName() {
            return null;
        }

        @Override
        public void close() throws IOException {
        }
    };

    // not requird as OF doesn't use it ?????
    @ModifyConstant(method = "loadTextureAtlas", constant = @Constant(stringValue = "Texture stitching"), require = 0)
    private String correctMessage(String original) {
        return "Texture loading";
    }

    /* Accelerate texture loading using similar strategy to ModernFix - drop PngSizeInfo entirely, defer height/width checks to later */
    @Redirect(method = { TEXTURE_LOADER_CORE, TEXTURE_LOADER_CORE_OF }, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/texture/PngSizeInfo;makeFromResource(Lnet/minecraft/client/resources/IResource;)Lnet/minecraft/client/renderer/texture/PngSizeInfo;"))
    private PngSizeInfo skipPngLoad(IResource resource) {
        return null;
    }

    @Redirect(method = { TEXTURE_LOADER_CORE, TEXTURE_LOADER_CORE_OF}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/resources/IResourceManager;getResource(Lnet/minecraft/util/ResourceLocation;)Lnet/minecraft/client/resources/IResource;"))
    private IResource skipResourceLoad(IResourceManager manager, ResourceLocation location) {
        return EMPTY_META_RESOURCE;
    }

    @Redirect(method = "generateMipmaps", at = @At(value = "INVOKE", target = "Lorg/apache/logging/log4j/Logger;error(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V", ordinal = 0, remap = false))
    private void skipErrorForWrongDimension(Logger logger, String msg, Object o1, Object o2, IResourceManager resourceManager, final TextureAtlasSprite texture) {
        ResourceLocation resourcelocation = this.getResourceLocation(texture);
        net.minecraftforge.fml.client.FMLClientHandler.instance().trackBrokenTexture(resourcelocation, ((RuntimeException)o2).getMessage());
    }

    @Redirect(method = "generateMipmaps", at = @At(value = "INVOKE", target = "Lorg/apache/logging/log4j/Logger;error(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V", ordinal = 1, remap = false))
    private void skipErrorForMissing(Logger logger, String msg, Object o1, Object o2, IResourceManager resourceManager, final TextureAtlasSprite texture) {
        ResourceLocation resourcelocation = this.getResourceLocation(texture);
        net.minecraftforge.fml.client.FMLClientHandler.instance().trackMissingTexture(resourcelocation);
    }

    private static final AtomicInteger loadedCount = new AtomicInteger(0);

    private static final Set<Class<?>> SAFE_CLASSES;

    static {
        ImmutableSet.Builder<Class<?>> builder = ImmutableSet.builder();
        for(String clzName : ImmutableList.of("net.minecraft.client.renderer.texture.TextureAtlasSprite")) {
            try {
                builder.add(Class.forName(clzName));
            } catch(ClassNotFoundException ignored) {}
        }
        SAFE_CLASSES = builder.build();
    }

    @Inject(method = "loadTextureAtlas", at = @At(value = "INVOKE", target = "Ljava/util/List;clear()V", ordinal = 0))
    private void preloadTextures(IResourceManager resourceManager, CallbackInfo ci) {
        /* parallel texture load go brr */
        Stopwatch watch = Stopwatch.createStarted();
        loadedCount.set(0);
        int numSubmittedSprites = 0;
        Object2IntOpenHashMap<Class<?>> invalidClasses = new Object2IntOpenHashMap<>();
        for(Map.Entry<String, TextureAtlasSprite> entry : mapRegisteredSprites.entrySet()) {
            TextureAtlasSprite sprite = entry.getValue();
            if(sprite != null) {
                if(!FMLClientHandler.instance().hasError() && SAFE_CLASSES.contains(sprite.getClass())) {
                    VintageFix.WORKER_POOL.execute(() -> {
                        try {
                            sprite.loadSprite(null, false);
                            ResourceLocation fileLocation = this.getResourceLocation(sprite);
                            try(IResource resource = resourceManager.getResource(fileLocation)) {
                                sprite.loadSpriteFrames(resource, this.mipmapLevels + 1);
                            }
                            sprite.generateMipmaps(this.mipmapLevels);
                        } catch(IOException | RuntimeException e) {
                            /* reset sprite state */
                            try { sprite.loadSprite(null, false); } catch(IOException ignored) {}
                        } finally {
                            loadedCount.incrementAndGet();
                        }
                    });
                    numSubmittedSprites++;
                } else {
                    invalidClasses.compute(sprite.getClass(), (k, v) -> v == null ? 1 : (v + 1));
                }
            }
        }
        int totalRegisteredSprites = mapRegisteredSprites.size();
        invalidClasses.forEach((clz, i) -> {
            VintageFix.LOGGER.warn("Can't preload sprite class {} (seen {}/{} times)", clz.getName(), i, totalRegisteredSprites);
        });
        ProgressManager.ProgressBar bar = ProgressManager.push("Preloading " + totalRegisteredSprites + " textures", 1);
        long timeToBlock = TimeUnit.MILLISECONDS.toNanos(30);
        while(loadedCount.get() < numSubmittedSprites) {
            LockSupport.parkNanos(timeToBlock);
        }
        watch.stop();
        VintageFix.LOGGER.info("Preloaded {} sprites in {}", numSubmittedSprites, watch);
        bar.step("done");
        ProgressManager.pop(bar);
    }

    @Redirect(method = { TEXTURE_LOADER_CORE, TEXTURE_LOADER_CORE_OF }, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;loadSprite(Lnet/minecraft/client/renderer/texture/PngSizeInfo;Z)V"))
    private void skipResetSprite(TextureAtlasSprite sprite, PngSizeInfo info, boolean flag) throws IOException {
        if(sprite.getClass() != TextureAtlasSprite.class || sprite.getFrameCount() == 0)
            sprite.loadSprite(info, flag);
    }

    @Inject(method = "generateMipmaps", at = @At("HEAD"), cancellable = true)
    private void skipPreloadedSprite(IResourceManager manager, TextureAtlasSprite sprite, CallbackInfoReturnable<Boolean> cir) {
        if(sprite.getClass() == TextureAtlasSprite.class && sprite.getFrameCount() > 0) {
            /* handled by preloader already */
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "generateMipmaps", at = @At("RETURN"), cancellable = true)
    private void skipStitchEmptySprite(IResourceManager manager, TextureAtlasSprite sprite, CallbackInfoReturnable<Boolean> cir) {
        if(cir.getReturnValueZ()) {
            try {
                sprite.getFrameTextureData(0);
            } catch(RuntimeException e) {
                VintageFix.LOGGER.warn("Skipped stitching empty sprite {}", sprite.getIconName());
                // data is missing, do not stitch
                cir.setReturnValue(false);
            }
        }
    }
}
