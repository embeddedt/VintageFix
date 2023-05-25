package org.embeddedt.vintagefix.dynamicresources.model;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.IRegistry;
import net.minecraftforge.client.model.ICustomModelLoader;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import org.embeddedt.vintagefix.VintageFix;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class DynamicModelProvider implements IRegistry<ResourceLocation, IModel> {
    //    private static final Logger LOGGER = LogManager.getLogger();
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

    private final Map<ResourceLocation, ResourceLocation> sideChannelAliases = new Object2ObjectOpenHashMap<>();

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
        try {
            return loadedModels.get(location, () -> Optional.ofNullable(loadModel(location, new LinkedHashSet<>()))).orElse(null);
        } catch (ExecutionException e) {
            throw new RuntimeException(e.getCause());
        }
    }

    private static final Map<ResourceLocation, IModel> MODEL_LOADER_REGISTRY_CACHE = ObfuscationReflectionHelper.getPrivateValue(ModelLoaderRegistry.class, null, "cache");

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
            model.getTextures();
        } catch (Exception e) {
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
}
