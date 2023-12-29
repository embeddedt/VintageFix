package org.embeddedt.vintagefix.mixin.dynamic_resources;

import com.google.common.collect.Sets;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.renderer.BlockModelShapes;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelBakery;
import net.minecraft.client.renderer.block.model.ModelManager;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.client.resources.SimpleReloadableResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.IRegistry;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.model.ICustomModelLoader;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.common.ForgeModContainer;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.ProgressManager;
import net.minecraftforge.fml.common.eventhandler.IEventListener;
import org.embeddedt.vintagefix.VintageFix;
import org.embeddedt.vintagefix.annotation.ClientOnlyMixin;
import org.embeddedt.vintagefix.dynamicresources.*;
import org.embeddedt.vintagefix.dynamicresources.model.DynamicBakedModelProvider;
import org.embeddedt.vintagefix.dynamicresources.model.DynamicModelProvider;
import org.embeddedt.vintagefix.dynamicresources.model.ModelLocationInformation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Pattern;

@Mixin(ModelManager.class)
@ClientOnlyMixin
public class MixinModelManager {
    @Shadow private IRegistry<ModelResourceLocation, IBakedModel> modelRegistry;
    @Shadow private IBakedModel defaultModel;
    @Shadow @Final private BlockModelShapes modelProvider;

    @Shadow
    @Final
    private TextureMap texMap;

    private Set<ResourceLocation> earlyDetectedTextures;

    private void doEarlyModelLoading(IResourceManager manager) {
        // load some models early (e.g. TConstruct)
        Predicate<String> shouldLoadEarly = p -> {
            if(p.length() < 6)
                return false;
            String customName = p.substring(0, p.length() - 5);
            return (customName.endsWith("tmat") || customName.endsWith("tcon") || customName.endsWith("mod") || customName.endsWith("conarm")) && p.endsWith(".json");
        };
        Predicate<String> shouldPersistEarly = p -> {
            return p.endsWith(".tmat.json");
        };

        Collection<String> earlyModelPaths = ResourcePackHelper.getAllPaths((SimpleReloadableResourceManager)manager, shouldLoadEarly);
        VintageFix.LOGGER.info("Early loading {} models", earlyModelPaths.size());

        int permLoaded = 0;

        for(String path : earlyModelPaths) {
            ResourceLocation rl = ResourcePackHelper.pathToResourceLocation(path, ResourcePackHelper.ResourceLocationMatchType.SHORT);
            if(rl != null) {
                try {
                    //VintageFix.LOGGER.info("Loading {}", rl);
                    IModel theModel = DynamicModelProvider.instance.getObject(rl);
                    if(theModel != null) {
                        try {
                            earlyDetectedTextures.addAll(theModel.getTextures());
                        } catch(RuntimeException ignored) {}
                    }
                    if(theModel != null && shouldPersistEarly.test(path)) {
                        DynamicModelProvider.instance.putObject(rl, theModel);
                        permLoaded++;
                    }
                } catch(Exception e) {
                    VintageFix.LOGGER.error("Early load error for {}", rl, e);
                }
            } else
                VintageFix.LOGGER.warn("Path {} is not a valid model location", path);
        }

        VintageFix.LOGGER.info("Permanently loaded {} models", permLoaded);
    }

    private boolean shouldLoadBlacklisted(ModelResourceLocation mrl) {
        return mrl.getNamespace().equals("thebetweenlands") || mrl.getNamespace().equals("dynamictrees");
    }

    private boolean shouldPersistBlacklisted(ResourceLocation mrl) {
        return false; // for now
    }

    private Map<ModelResourceLocation, IModel> blackListedModels;

    private void doBlacklistedModelLoading(IResourceManager manager) {
        blackListedModels = new Object2ObjectOpenHashMap<>();
        List<ResourceLocation> modelList = new ArrayList<>();
        for(ModelResourceLocation mrl : ModelLocationInformation.inventoryVariantLocations.keySet()) {
            if(shouldLoadBlacklisted(mrl))
                modelList.add(mrl);
        }
        for(Collection<ModelResourceLocation> c : ModelLocationInformation.validVariantsForBlock.values()) {
            for(ModelResourceLocation mrl : c) {
                if(shouldLoadBlacklisted(mrl))
                    modelList.add(mrl);
            }
        }
        if(Loader.isModLoaded("gbook"))
            modelList.add(new ResourceLocation("gbook", "block/custom/book")); // ensure book textures are baked
        if(modelList.size() == 0)
            return;
        ProgressManager.ProgressBar bar = ProgressManager.push("Incompatible model loading", modelList.size());
        int errors = 0;
        for(ResourceLocation mrl : modelList) {
            bar.step(mrl.toString());
            try {
                IModel model = DynamicModelProvider.instance.getObject(mrl);
                if(model != null) {
                    try {
                        earlyDetectedTextures.addAll(model.getTextures());
                    } catch(RuntimeException ignored) {}
                    if(shouldPersistBlacklisted(mrl))
                        DynamicModelProvider.instance.putObject(mrl, model);
                    if(mrl instanceof ModelResourceLocation)
                        blackListedModels.put((ModelResourceLocation)mrl, model);
                }
            } catch(RuntimeException e) {
                VintageFix.LOGGER.error("Error loading blacklisted model {}: {}", mrl, e);
                errors++;
            }
        }
        VintageFix.LOGGER.info("{}/{} models had errors loading", errors, modelList.size());
        ProgressManager.pop(bar);
    }

    private void doBlacklistedModelBaking(IResourceManager manager) {
        if(blackListedModels.size() > 0) {
            ProgressManager.ProgressBar bar = ProgressManager.push("Incompatible model baking", blackListedModels.size());
            for(Map.Entry<ModelResourceLocation, IModel> entry : blackListedModels.entrySet()) {
                bar.step(entry.getKey().toString());
                try {
                    IBakedModel model = DynamicBakedModelProvider.instance.getObject(entry.getKey());
                    if(shouldPersistBlacklisted(entry.getKey()))
                        DynamicBakedModelProvider.instance.putObject(entry.getKey(), model);
                } catch(RuntimeException e) {
                    VintageFix.LOGGER.error("Error baking blacklisted model {}: {}", entry.getKey(), e);
                }
            }
            ProgressManager.pop(bar);
        }
        blackListedModels = null;
    }

    /**
     * @author embeddedt, Runemoro
     * @reason Don't set up the ModelLoader. Instead, set up the caching DynamicModelProvider
     * and DynamicBakedModelProviders, which will act as the model registry.
     */
    @Overwrite
    public void onResourceManagerReload(IResourceManager resourceManager) {
        TextureCollector.startDiscovery();
        ItemBakeThread.stopAndJoin();
        // Run the "end of model loading" listeners first
        for(IResourceManagerReloadListener listener : DeferredListeners.deferredListeners) {
            listener.onResourceManagerReload(resourceManager);
        }

        // Generate information about model locations, such as the blockstate location to block map
        // and the item variant to model location map.

        ModelLoader loader = new ModelLoader(resourceManager, texMap, modelProvider);

        ProgressManager.ProgressBar overallBar = ProgressManager.push("Setting up dynamic models", 5);
        overallBar.step("Generate model locations");
        ModelLocationInformation.init(loader, modelProvider.getBlockStateMapper());


        // Get custom loaders
        Set<ICustomModelLoader> loaders;
        try {
            Field loadersField = ModelLoaderRegistry.class.getDeclaredField("loaders");
            loadersField.setAccessible(true);
            // noinspection unchecked
            loaders = (Set<ICustomModelLoader>) loadersField.get(null);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }

        // Create the dynamic model and bake model providers
        DynamicModelProvider dynamicModelProvider = new DynamicModelProvider(loaders);
        DynamicModelProvider.instance = dynamicModelProvider;
        DynamicBakedModelProvider dynamicBakedModelProvider = new DynamicBakedModelProvider(dynamicModelProvider);
        DynamicBakedModelProvider.instance = dynamicBakedModelProvider;
        modelRegistry = dynamicBakedModelProvider;
        SafeModelBakeWrapper.theManager = (ModelManager)(Object)this;

        earlyDetectedTextures = new HashSet<>();

        overallBar.step("Early model loading");
        doEarlyModelLoading(resourceManager);
        overallBar.step("Blacklisted model loading");
        // now do the blacklisted model loading
        doBlacklistedModelLoading(resourceManager);

        Method getTexturesMethod = ObfuscationReflectionHelper.findMethod(ModelLoaderRegistry.class, "getTextures", Iterable.class);
        try {
            getTexturesMethod.setAccessible(true);
            Iterable<ResourceLocation> registryTextures = (Iterable<ResourceLocation>)getTexturesMethod.invoke(null);
            registryTextures.forEach(earlyDetectedTextures::add);
        } catch(ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        earlyDetectedTextures.remove(TextureMap.LOCATION_MISSING_TEXTURE);
        earlyDetectedTextures.addAll(ObfuscationReflectionHelper.getPrivateValue(ModelBakery.class, null, "field_177602_b"));

        overallBar.step("Load textures");
        texMap.loadSprites(resourceManager, map -> earlyDetectedTextures.forEach(map::registerSprite));

        // Get the default model, returned by getModel when the model provider returns null
        defaultModel = modelRegistry.getObject(DynamicBakedModelProvider.MISSING_MODEL_LOCATION);
        if(defaultModel == null)
            throw new AssertionError("Missing model is missing");
        DynamicBakedModelProvider.missingModel = defaultModel;

        doBlacklistedModelBaking(resourceManager);

        // Register the universal bucket item
        if (FluidRegistry.isUniversalBucketEnabled()) {
            ModelLoader.setBucketModelDefinition(ForgeModContainer.getInstance().universalBucket);
        }

        // Post the event, but just log an error if a listener throws an exception.
        ModelBakeEvent event = new ModelBakeEvent((ModelManager) (Object) this, new WrappingModelRegistry(modelRegistry), loader);
        IEventListener[] listeners = EventUtil.getListenersForEvent(event);
        overallBar.step("Baking");
        ProgressManager.ProgressBar bakeEventBar = ProgressManager.push("Posting bake events", listeners.length);
        for (IEventListener listener : listeners) {
            bakeEventBar.step(listener.toString());
            try {
                listener.invoke(event);
            } catch (Throwable t) {
                VintageFix.LOGGER.error(event + " listener '" + listener + "' threw exception, models may be broken", t);
            }
        }
        ProgressManager.pop(bakeEventBar);
        ProgressManager.pop(overallBar);
        // Make the model provider load blockstate to model information. See MixinBlockModelShapes
        modelProvider.reloadModels();
        earlyDetectedTextures = new HashSet<>();
    }
}
