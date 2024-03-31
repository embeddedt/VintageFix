package org.embeddedt.vintagefix.dynamicresources.model;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.block.state.IBlockState;
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
import org.embeddedt.vintagefix.dynamicresources.IBlockModelShapes;
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

    private final DynamicModelProvider modelProvider;
    private final Map<ModelResourceLocation, IBakedModel> permanentlyLoadedBakedModels = Collections.synchronizedMap(new Object2ObjectOpenHashMap<>());
    private final Cache<ModelResourceLocation, Optional<IBakedModel>> loadedBakedModels =
            CacheBuilder.newBuilder()
                        .expireAfterAccess(3, TimeUnit.MINUTES)
                        .maximumSize(1000)
                        .concurrencyLevel(8)
                        .softValues()
                        .build();

    private final BakedModelStore bakedModelStore = new BakedModelStore();

    public DynamicBakedModelProvider(DynamicModelProvider modelProvider) {
        this.modelProvider = modelProvider;
    }

    public Map<IBlockState, IBakedModel> getBakedModelStore() {
        return this.bakedModelStore;
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
        Optional<IBakedModel> opt = loadedBakedModels.getIfPresent(location);
        if(opt == null) {
            synchronized (this) {
                opt = loadedBakedModels.getIfPresent(location);
                if(opt == null) {
                    opt = Optional.ofNullable(loadBakedModel(location));
                    loadedBakedModels.put(location, opt);
                }
            }
        }
        // avoid lambda allocation
        if(opt.isPresent())
            return opt.get();
        else
            return getMissingIfRegistered(location);
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
        /* emulate non-null for known variants */
        Collection<ModelResourceLocation> knownVariants = ModelLocationInformation.validVariantsForBlock.get(new ResourceLocation(location.getNamespace(), location.getPath()));
        return (knownVariants != null && knownVariants.contains(location)) ? missingModel : null;
    }

    private static final Function<ResourceLocation, TextureAtlasSprite> loggingTextureGetter = location -> {
        TextureMap map = Minecraft.getMinecraft().getTextureMapBlocks();
        String name = location.toString();
        TextureAtlasSprite sprite = map.getAtlasSprite(name);
        // validate that its not an explicit request for missingno
        if(sprite == map.getMissingSprite() && !sprite.getIconName().equals(name) && !(location.getNamespace().equals("minecraft") && sprite.getIconName().equals(location.getPath())) && !name.equals("minecraft:builtin/white")) {
            LOGGER.warn("Texture {} was not discovered during texture pass", name);
        }
        return sprite;
    };

    private static IBakedModel bakeAndCheckTextures(ResourceLocation location, IModel model, VertexFormat format) {
        IBakedModel bakedModel = model.bake(model.getDefaultState(), format, loggingTextureGetter);
        if(!MISSING_MODEL_LOCATION.equals(location)) {
            DynamicModelBakeEvent event = new DynamicModelBakeEvent(location, model, bakedModel);
            MinecraftForge.EVENT_BUS.post(event);
            return event.bakedModel;
        } else
            return bakedModel;
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

    class BakedModelStore implements Map<IBlockState, IBakedModel> {

        @Override
        public int size() {
            return DynamicBakedModelProvider.this.permanentlyLoadedBakedModels.size();
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public boolean containsKey(Object key) {
            return true;
        }

        @Override
        public boolean containsValue(Object value) {
            return true;
        }

        @Override
        public IBakedModel get(Object key) {
            if(key instanceof IBlockState) {
                return Minecraft.getMinecraft().getBlockRendererDispatcher().getBlockModelShapes().getModelForState((IBlockState)key);
            } else {
                return null;
            }
        }

        @Override
        public IBakedModel put(IBlockState key, IBakedModel value) {
            ModelResourceLocation mrl = ((IBlockModelShapes) Minecraft.getMinecraft().getBlockRendererDispatcher().getBlockModelShapes()).getLocationForState(key);
            DynamicBakedModelProvider.this.putObject(mrl, value);
            return null;
        }

        @Override
        public IBakedModel remove(Object key) {
            return null;
        }

        @Override
        public void putAll(Map<? extends IBlockState, ? extends IBakedModel> m) {
            m.forEach(this::put);
        }

        @Override
        public void clear() {

        }

        @Override
        public Set<IBlockState> keySet() {
            return Collections.emptySet();
        }

        @Override
        public Collection<IBakedModel> values() {
            return DynamicBakedModelProvider.this.permanentlyLoadedBakedModels.values();
        }

        @Override
        public Set<Entry<IBlockState, IBakedModel>> entrySet() {
            return Collections.emptySet();
        }
    }
}
