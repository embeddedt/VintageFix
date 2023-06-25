package org.embeddedt.vintagefix.dynamicresources.model;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelBlock;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.resources.IResource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.IRegistry;
import net.minecraft.util.registry.RegistrySimple;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.common.MinecraftForge;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.embeddedt.vintagefix.VintageFix;
import org.embeddedt.vintagefix.event.DynamicModelBakeEvent;
import org.embeddedt.vintagefix.util.ExceptionHelper;

import javax.annotation.Nullable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class DynamicBakedModelProvider extends RegistrySimple<ModelResourceLocation, IBakedModel> {
    private static final Logger LOGGER = LogManager.getLogger();
    public static DynamicBakedModelProvider instance;
    public static IBakedModel missingModel;
    public static final ModelResourceLocation MISSING_MODEL_LOCATION = new ModelResourceLocation("builtin/missing", "missing");

    private final IRegistry<ResourceLocation, IModel> modelProvider;
    private final Map<ModelResourceLocation, IBakedModel> permanentlyLoadedBakedModels = Collections.synchronizedMap(new Object2ObjectOpenHashMap<>());
    private final Cache<ModelResourceLocation, Optional<IBakedModel>> loadedBakedModels =
            CacheBuilder.newBuilder()
                        .expireAfterAccess(3, TimeUnit.MINUTES)
                        .maximumSize(1000)
                        .concurrencyLevel(8)
                        .softValues()
                        .build();

    public DynamicBakedModelProvider(DynamicModelProvider modelProvider) {
        this.modelProvider = modelProvider;
    }

    @Override
    protected Map<ModelResourceLocation, IBakedModel> createUnderlyingMap() {
        return ImmutableMap.of();
    }

    @Nullable
    private static IBakedModel getMissingIfRegistered(ModelResourceLocation location) {
        /* check if its known inventory variant */
        if(ModelLocationInformation.allItemVariants.contains(location))
            return missingModel;
        else
            return null;
    }

    @Override
    @Nullable
    public IBakedModel getObject(ModelResourceLocation location) {
        try {
            return loadedBakedModels.get(location, () -> Optional.ofNullable(loadBakedModel(location))).orElse(getMissingIfRegistered(location));
        } catch (ExecutionException e) {
            throw new RuntimeException(e.getCause());
        }
    }

    @Nullable
    public IBakedModel getModelIfPresent(ModelResourceLocation location) {
        Optional<IBakedModel> opt = loadedBakedModels.getIfPresent(location);
        if(opt != null)
            return opt.orElse(null);
        else
            return null;
    }

    private static final FileNotFoundException PARENT_MISSING_EXCEPTION = new FileNotFoundException("Failed to load model parent");

    private IBakedModel loadBakedModel(ModelResourceLocation location) {
//        LOGGER.info("Loading baked model " + location);

        IBakedModel bakedModel = permanentlyLoadedBakedModels.get(location);
        if (bakedModel != null) {
            return bakedModel;
        }

        Throwable mException = null;
        IModel model = null;
        try {
            model = modelProvider.getObject(location);
        } catch(Throwable e) {
            mException = e;
        }

        if(model == null) {
            /* see if anyone injects */
            DynamicModelBakeEvent event = new DynamicModelBakeEvent(location, null, missingModel);
            MinecraftForge.EVENT_BUS.post(event);
            if(event.bakedModel != missingModel)
                return event.bakedModel;

            if(ModelLocationInformation.canLogError(location.getNamespace())) {
                if(!ModelLocationInformation.DEBUG_MODEL_LOAD)
                    LOGGER.error("Error occured while loading model {}", location);
                else {
                    LOGGER.error("Failed to load model {}", location, mException);
                }
            }
        } else {
            try {
                return bakeAndCheckTextures(location, model, DefaultVertexFormats.ITEM);
            } catch (Throwable t) {
                if(ModelLocationInformation.canLogError(location.getNamespace())) {
                    if(ModelLocationInformation.DEBUG_MODEL_LOAD)
                        LOGGER.error("Error occured while baking model {}", location, t);
                    else
                        LOGGER.error("Error occured while baking model {}", location);
                }
            }
        }
        return null;
    }

    private static final Function<ResourceLocation, TextureAtlasSprite> loggingTextureGetter = location -> {
        TextureMap map = Minecraft.getMinecraft().getTextureMapBlocks();
        String name = location.toString();
        TextureAtlasSprite sprite = map.getAtlasSprite(name);
        // validate that its not an explicit request for missingno
        if(sprite == map.getMissingSprite() && !sprite.getIconName().equals(name) && !(location.getNamespace().equals("minecraft") && sprite.getIconName().equals(location.getPath()))) {
            LOGGER.warn("Texture {} was not discovered during texture pass", name);
        }
        return sprite;
    };

    private static IBakedModel bakeAndCheckTextures(ResourceLocation location, IModel model, VertexFormat format) {
        // TODO log when textures missing
        synchronized (DynamicBakedModelProvider.class) {
            IBakedModel bakedModel = model.bake(model.getDefaultState(), format, loggingTextureGetter);
            if(!MISSING_MODEL_LOCATION.equals(location)) {
                DynamicModelBakeEvent event = new DynamicModelBakeEvent(location, model, bakedModel);
                MinecraftForge.EVENT_BUS.post(event);
                return event.bakedModel;
            } else
                return bakedModel;
        }
    }

    @Override
    public void putObject(ModelResourceLocation key, IBakedModel value) {
        permanentlyLoadedBakedModels.put(key, value);
        loadedBakedModels.invalidate(key);
    }

    @Override
    public Set<ModelResourceLocation> getKeys() {
        return permanentlyLoadedBakedModels.keySet();
    }

    @Override
    public Iterator<IBakedModel> iterator() {
        return permanentlyLoadedBakedModels.values().iterator();
    }

    public void invalidateThrough(ModelResourceLocation key) {
        loadedBakedModels.invalidate(key);
        ((DynamicModelProvider)modelProvider).invalidate(key);
    }
}
