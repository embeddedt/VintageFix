package org.embeddedt.vintagefix.dynamicresources.model;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.IRegistry;
import net.minecraftforge.client.model.ICustomModelLoader;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.embeddedt.vintagefix.VintageFix;
import org.embeddedt.vintagefix.core.MixinConfigPlugin;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class DynamicModelProvider implements IRegistry<ResourceLocation, IModel> {
    private static final Logger LOGGER = LogManager.getLogger();
    public static DynamicModelProvider instance;

    private final Set<ICustomModelLoader> loaders;
    private final Map<ResourceLocation, IModel> permanentlyLoadedModels = new Object2ObjectOpenHashMap<>();
    private final Cache<ResourceLocation, Optional<IModel>> loadedModels =
            CacheBuilder.newBuilder()
                        .expireAfterAccess(3, TimeUnit.MINUTES)
                        .maximumSize(1000)
                        .concurrencyLevel(8)
                        .softValues()
                        .build();

    public static Set<ResourceLocation> textureCapturer = null;

    private final Map<ResourceLocation, ResourceLocation> sideChannelAliases = new Object2ObjectOpenHashMap<>();
    public static final boolean HIDE_MODEL_ERRORS = MixinConfigPlugin.isMixinClassApplied("mixin.dynamic_resources.hide_model_exceptions.ModelErrorMixin");

    public DynamicModelProvider(Set<ICustomModelLoader> loaders) {
        this.loaders = loaders;
        sideChannelAliases.put(new ResourceLocation("block/builtin/entity"), new ResourceLocation("builtin/entity"));
    }

    private static final ICustomModelLoader VANILLA_LOADER, VARIANT_LOADER;

    static {
        try {
            VANILLA_LOADER = ObfuscationReflectionHelper.<ICustomModelLoader, ICustomModelLoader>getPrivateValue((Class<? super ICustomModelLoader>)Class.forName("net.minecraftforge.client.model.ModelLoader$VanillaLoader"), null, "INSTANCE");
            VARIANT_LOADER = ObfuscationReflectionHelper.<ICustomModelLoader, ICustomModelLoader>getPrivateValue((Class<? super ICustomModelLoader>)Class.forName("net.minecraftforge.client.model.ModelLoader$VariantLoader"), null, "INSTANCE");
        } catch(ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Nullable
    @Override
    public IModel getObject(ResourceLocation location) {
        Optional<IModel> opt = loadedModels.getIfPresent(location);
        if(opt == null) {
            synchronized (this) {
                opt = loadedModels.getIfPresent(location);
                if(opt == null) {
                    try {
                        opt = Optional.ofNullable(loadModelFromBlockstateOrInventory(location));
                    } catch(Exception e) {
                        throw new RuntimeException(e);
                    }
                    if(textureCapturer != null && opt.isPresent()) {
                        textureCapturer.addAll(opt.get().getTextures());
                    }
                    loadedModels.put(location, opt);
                }
            }
        }
        return opt.orElse(null);
    }

    public void clearCache() {
        loadedModels.invalidateAll();
    }

    public IModel getModelOrMissing(ResourceLocation location) {
        try {
            return getObject(location);
        } catch(RuntimeException e) {
            return getObject(new ModelResourceLocation("builtin/missing", "missing"));
        }
    }

    private static final Map<ResourceLocation, IModel> MODEL_LOADER_REGISTRY_CACHE = ObfuscationReflectionHelper.getPrivateValue(ModelLoaderRegistry.class, null, "cache");

    private static final Class<?> VANILLA_MODEL_WRAPPER;

    static {
        try {
            VANILLA_MODEL_WRAPPER = Class.forName("net.minecraftforge.client.model.ModelLoader$VanillaModelWrapper");
        } catch(ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private IModel loadModelFromBlockstateOrInventory(ResourceLocation location) throws ModelLoaderRegistry.LoaderException {
        ResourceLocation inventoryVariantLocation = null;
        Throwable blockStateException = null, normalException = null;
        IModel model = null;

        // first, attempt to fetch from the blockstate (by using the MRL)
        try {
            model = loadModel(location, new LinkedHashSet<>());
        } catch(Throwable e) {
            blockStateException = e;
            if(location instanceof ModelResourceLocation) {
                // check if an inventory variant is registered, and if so, try that
                inventoryVariantLocation = ModelLocationInformation.getInventoryVariantLocation((ModelResourceLocation)location);
                if(inventoryVariantLocation == null)
                    inventoryVariantLocation = new ResourceLocation(location.getNamespace(), location.getPath());
                try {
                    model = loadModel(inventoryVariantLocation, new LinkedHashSet<>());
                    if (VANILLA_MODEL_WRAPPER.isAssignableFrom(model.getClass())) {
                        for (ResourceLocation dep : model.asVanillaModel().get().getOverrideLocations()) {
                            if (!location.equals(dep)) {
                                ModelLocationInformation.addInventoryVariantLocation(ModelLocationInformation.getInventoryVariant(dep.toString()), dep);
                            }
                        }
                    }
                } catch(Throwable e2) {
                    normalException = e;
                }
            }
        }

        if(model == null) {
            ModelLoaderRegistry.LoaderException theException = new ModelLoaderRegistry.LoaderException("Model loading failure for " + location);
            if(blockStateException != null)
                theException.addSuppressed(blockStateException);
            if(normalException != null)
                theException.addSuppressed(normalException);
            if(HIDE_MODEL_ERRORS) {
                blockStateException = null;
                normalException = null;
            }
            if(ModelLocationInformation.canLogError(location.getNamespace())) {
                LOGGER.error("Failed to load model {}", location, blockStateException);
                if(normalException != null)
                    LOGGER.error("Failed to load model {} as item {}", location, inventoryVariantLocation, normalException);
            }
            throw theException;
        }
        return model;
    }

    private IModel loadModel(ResourceLocation location, Set<ResourceLocation> loadStack) throws ModelLoaderRegistry.LoaderException {
        if(loadStack.add(location)) {
            ResourceLocation alias;
            synchronized (sideChannelAliases) {
                alias = sideChannelAliases.get(location);
            }
            if(alias != null)
                return loadModel(alias, loadStack);
        }
        IModel model = permanentlyLoadedModels.get(location);
        if (model != null) {
            return model;
        }

        model = MODEL_LOADER_REGISTRY_CACHE.get(location);
        if(model != null)
            return model;

        if(ModelLocationInformation.DEBUG_MODEL_LOAD)
            VintageFix.LOGGER.info("Loading model {}", location);

        // Check if a custom loader accepts the model
        ResourceLocation actualLocation = ModelLoaderRegistry.getActualLocation(location);
        ICustomModelLoader accepted = null;
        for (ICustomModelLoader loader : loaders) {
            try {
                if (loader.accepts(actualLocation)) {
                    if (accepted != null) {
                        throw new ModelLoaderRegistry.LoaderException("Loaders (" + accepted + " and " + loader + ") both accept model " + location);
                    }
                    accepted = loader;
                }
            } catch (Exception e) {
                throw new ModelLoaderRegistry.LoaderException("Exception checking if model " + location + " can be loaded with loader " + loader, e);
            }
        }

        // No custom loaders found, use vanilla loaders
        if (accepted == null) {
            boolean isBuiltin = actualLocation.getPath().startsWith("builtin/") || actualLocation.getPath().startsWith("block/builtin/") || actualLocation.getPath().startsWith("item/builtin/");
            if (!isBuiltin && VARIANT_LOADER.accepts(actualLocation)) {
                accepted = VARIANT_LOADER;
            } else if (VANILLA_LOADER.accepts(actualLocation)) {
                accepted = VANILLA_LOADER;
            }
        }

        if (accepted == null) {
            throw new ModelLoaderRegistry.LoaderException("No suitable loader found for the model " + location);
        }

        try {
            model = accepted.loadModel(actualLocation);
        } catch (Exception e) {
            throw new ModelLoaderRegistry.LoaderException("Exception loading model " + location + " with loader " + accepted, e);
        }

        if(model == null)
            throw new ModelLoaderRegistry.LoaderException("Loader " + accepted + " provided null model for " + location);

        if(model == ModelLoaderRegistry.getMissingModel() && !location.equals(DynamicBakedModelProvider.MISSING_MODEL_LOCATION))
            throw new ModelLoaderRegistry.LoaderException("Loader " + accepted + " provided missing model for " + location);

        try {
            model.getTextures();
        } catch(Exception e) {
            throw new ModelLoaderRegistry.LoaderException("Exception loading model " + location + " with loader " + accepted, e);
        }

        return model;
    }

    @Override
    public void putObject(ResourceLocation key, IModel value) {
        permanentlyLoadedModels.put(key, value);
        loadedModels.invalidate(key);
    }

    public void putAlias(ResourceLocation original, ResourceLocation to) {
        synchronized (sideChannelAliases) {
            sideChannelAliases.put(original, to);
        }
    }

    @Override
    public Set<ResourceLocation> getKeys() {
        return permanentlyLoadedModels.keySet();
    }

    @Override
    public Iterator<IModel> iterator() {
        return permanentlyLoadedModels.values().iterator();
    }

    public void invalidate(ResourceLocation key) {
        loadedModels.invalidate(key);
    }
}
