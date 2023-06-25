package org.embeddedt.vintagefix;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.resources.*;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ColorHandlerEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.ProgressManager;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.commons.io.IOUtils;
import org.embeddedt.vintagefix.core.MixinConfigPlugin;
import org.embeddedt.vintagefix.core.VintageFixCore;
import org.embeddedt.vintagefix.dynamicresources.CTMHelper;
import org.embeddedt.vintagefix.dynamicresources.IWeakTextureMap;
import org.embeddedt.vintagefix.dynamicresources.ResourcePackHelper;
import org.embeddedt.vintagefix.dynamicresources.model.ModelLocationInformation;
import org.embeddedt.vintagefix.impl.Deduplicator;
import org.embeddedt.vintagefix.util.Util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.*;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class VintageFixClient {
    public VintageFixClient() {
        ObfuscationReflectionHelper.setPrivateValue(Minecraft.class, Minecraft.getMinecraft(), new byte[0], "field_71444_a");
        if(Loader.isModLoaded("ctm")) {
            MinecraftForge.EVENT_BUS.register(CTMHelper.class);
        }
        if(VintageFixCore.OPTIFINE)
            VintageFix.LOGGER.fatal("OptiFine detected, there may be issues");
    }

    public static boolean modernityPresent = false;

    @SubscribeEvent
    public void registerListener(ColorHandlerEvent.Block event) {
        Deduplicator.registerReloadListener();
        ((SimpleReloadableResourceManager)Minecraft.getMinecraft().getResourceManager()).registerReloadListener(new IResourceManagerReloadListener() {
            @Override
            public void onResourceManagerReload(IResourceManager resourceManager) {
                List<IResourcePack> theList = getResourcePackList();
                modernityPresent = theList.stream().anyMatch(pack -> pack.getPackName().toLowerCase(Locale.ROOT).contains("modernity"));
            }
        });
    }

    // target all textures in the listed subfolders, or textures in the root folder
    private static final Pattern TEXTURE_MATCH_PATTERN = Pattern.compile("^/?assets/(.+?(?=/))/textures/((?:attachment|aspect.?|bettergrass|block.?|cape|customoverlay|decors|item.?|entity/(armor|bed|chest)|fluid.?|model.?|part.?|pipe|rendering|ropebridge|slot.?|solid_block|tile.?|tinkers|valuetype.?)/.*)\\.png$");

    private void registerSpriteSafe(TextureMap map, ResourceLocation location) {
        try {
            ((IWeakTextureMap)map).registerSpriteWeak(location);
        } catch(RuntimeException ignored) {
        }
    }

    private static final ImmutableListMultimap<String, ResourceLocation> EXTRA_TEXTURES_BY_MOD = ImmutableListMultimap.<String, ResourceLocation>builder()
        .put("mekanism", new ResourceLocation("mekanism", "entities/robit"))
        .build();

    static List<IResourcePack> resourcePackList = ImmutableList.of();

    private static List<IResourcePack> getResourcePackList() {
        Set<IResourcePack> resourcePacks = new LinkedHashSet<>();
        SimpleReloadableResourceManager manager = (SimpleReloadableResourceManager)Minecraft.getMinecraft().getResourceManager();
        Map<String, FallbackResourceManager> domainManagers = ObfuscationReflectionHelper.getPrivateValue(SimpleReloadableResourceManager.class, manager, "field_110548_a");
        for(FallbackResourceManager fallback : domainManagers.values()) {
            List<IResourcePack> fallbackPacks = ObfuscationReflectionHelper.getPrivateValue(FallbackResourceManager.class, fallback, "field_110540_a");
            resourcePacks.addAll(fallbackPacks);
        }
        return new ArrayList<>(resourcePacks);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void collectTextures(TextureStitchEvent.Pre event) {
        if(MixinConfigPlugin.isMixinClassApplied("mixin.dynamic_resources.TextureCollectionMixin")) {
            /* take every texture from these folders (1.19.3+ emulation) */
            Stopwatch watch = Stopwatch.createStarted();
            TextureMap map = event.getMap();
            resourcePackList = getResourcePackList();
            CompletableFuture<List<ResourceLocation>> modelTextures = CompletableFuture.supplyAsync(VintageFixClient::collectModelTextures, VintageFix.WORKER_POOL);
            int numFoundSprites = 0;
            String[] gameFolders = new String[] { "resources", "oresources" };
            ProgressManager.ProgressBar textureBar = ProgressManager.push("Scanning resource packs", resourcePackList.size() + gameFolders.length + 1);
            for(IResourcePack pack : resourcePackList) {
                textureBar.step(pack.getPackName());
                try {
                    Collection<String> paths = ResourcePackHelper.getAllPaths(pack, s -> true);
                    for(String path : paths) {
                        Matcher matcher = TEXTURE_MATCH_PATTERN.matcher(path);
                        if(matcher.matches()) {
                            registerSpriteSafe(map, new ResourceLocation(matcher.group(1), matcher.group(2)));
                            numFoundSprites++;
                        }
                    }
                } catch(IOException e) {
                    VintageFix.LOGGER.error("Error listing resources", e);
                }
            }

            VintageFix.LOGGER.info("Found {} sprites (some possibly duplicated among resource packs)", numFoundSprites);
            Path gameDirPath = Minecraft.getMinecraft().gameDir.toPath();
            for(String gameFolder : gameFolders) {
                textureBar.step(gameFolder);
                Path base = gameDirPath.resolve(gameFolder);
                try(Stream<Path> stream = Files.walk(base)) {
                    Iterator<String> iterator = stream.map(base::relativize).map(path -> "assets/" + Util.normalizePathToString(path)).iterator();
                    while(iterator.hasNext()) {
                        String p = iterator.next();
                        Matcher matcher = TEXTURE_MATCH_PATTERN.matcher(p);
                        if(matcher.matches()) {
                            registerSpriteSafe(map, new ResourceLocation(matcher.group(1), matcher.group(2)));
                            numFoundSprites++;
                        }
                    }
                } catch(FileNotFoundException | NoSuchFileException ignored) {
                } catch(IOException e) {
                    VintageFix.LOGGER.error("Error listing resources", e);
                }
            }

            for(Map.Entry<String, ResourceLocation> entry : EXTRA_TEXTURES_BY_MOD.entries()) {
                registerSpriteSafe(map, entry.getValue());
                numFoundSprites++;
            }
            textureBar.step("Model scanning");
            for(ResourceLocation location : modelTextures.join()) {
                registerSpriteSafe(map, location);
                numFoundSprites++;
            }
            // register all fluid textures
            for(Fluid f : FluidRegistry.getRegisteredFluids().values()) {
                if(f.getStill() != null)
                    registerSpriteSafe(map, f.getStill());
                if(f.getFlowing() != null)
                    registerSpriteSafe(map, f.getFlowing());
            }
            ProgressManager.pop(textureBar);
            watch.stop();
            VintageFix.LOGGER.info("Texture search took {}, total of {} collected sprites", watch, numFoundSprites);
        }
    }

    static Set<ResourceLocation> lookedAtLocations = Collections.synchronizedSet(new ObjectOpenHashSet<>());
    static Map<ResourceLocation, Boolean> doesResourceExist = new ConcurrentHashMap<>();

    private static List<ResourceLocation> collectModelTextures() {
        ObjectOpenHashSet<ResourceLocation> allBlockstates = new ObjectOpenHashSet<>();
        Consumer<ModelResourceLocation> adder = mrl -> allBlockstates.add(new ResourceLocation(mrl.getNamespace(), mrl.getPath()));
        ModelLocationInformation.allItemVariants.forEach(adder);
        for(Collection<ModelResourceLocation> collection : ModelLocationInformation.validVariantsForBlock.values()) {
            collection.forEach(adder);
        }
        List<CompletableFuture<List<ResourceLocation>>> results = new ArrayList<>();
        for(ResourceLocation location : allBlockstates) {
            results.add(CompletableFuture.supplyAsync(() -> {
                ResourceLocation blockstateLocation = new ResourceLocation(location.getNamespace(), "blockstates/" + location.getPath() + ".json");
                return collectJsonTexture(blockstateLocation);
            }, VintageFix.WORKER_POOL));
        }
        for(ResourceLocation location : ModelLocationInformation.inventoryVariantLocations.values()) {
            results.add(CompletableFuture.supplyAsync(() -> {
                ResourceLocation modelLocation = new ResourceLocation(location.getNamespace(), "models/" + location.getPath() + ".json");
                return collectJsonTexture(modelLocation);
            }, VintageFix.WORKER_POOL));
        }
        CompletableFuture.allOf(results.toArray(new CompletableFuture[0])).join();
        Set<ResourceLocation> finalResults = new ObjectOpenHashSet<>();
        for(CompletableFuture<List<ResourceLocation>> future : results) {
            List<ResourceLocation> list = future.join();
            finalResults.addAll(list);
        }
        lookedAtLocations.clear();
        doesResourceExist.clear();
        return new ArrayList<>(finalResults);
    }

    private static final Pattern JSON_TEXTURE_MATCHER = Pattern.compile("\"(?:([A-Za-z0-9_\\-.]+):|)([A-za-z0-9_\\-./]+)\"");

    private static boolean resourceExists(ResourceLocation loc) {
        return doesResourceExist.computeIfAbsent(loc, rl -> {
            try(IResource ignored1 = Minecraft.getMinecraft().getResourceManager().getResource(rl)) {
                return true;
            } catch(IOException ignored) {
            }
            return false;
        });
    }

    private static List<ResourceLocation> collectJsonTexture(ResourceLocation jsonLocation) {
        // avoid re-checking the same location many times
        if(!lookedAtLocations.add(jsonLocation))
            return ImmutableList.of();
        IResourceManager manager = Minecraft.getMinecraft().getResourceManager();
        try(IResource resource = manager.getResource(jsonLocation)) {
            InputStream stream = resource.getInputStream();
            String str = IOUtils.toString(stream, StandardCharsets.UTF_8);
            Matcher matcher = JSON_TEXTURE_MATCHER.matcher(str);
            List<ResourceLocation> textureLoc = new ArrayList<>();
            while(matcher.find()) {
                String namespace = matcher.group(1);
                if(namespace == null) {
                    namespace = "minecraft";
                }
                String path = matcher.group(2);
                // check if it's a texture
                if(resourceExists(new ResourceLocation(namespace, "textures/" + path + ".png"))) {
                    ResourceLocation realLocation = new ResourceLocation(namespace, path);
                    textureLoc.add(realLocation);
                } else {
                    // check if it's a blockstate-referenced model
                    ResourceLocation modelLocation = new ResourceLocation(namespace, "models/block/" + path + ".json");
                    if(resourceExists(modelLocation))
                        textureLoc.addAll(collectJsonTexture(modelLocation));
                    else {
                        // check if it's a model
                        modelLocation = new ResourceLocation(namespace, "models/" + path + ".json");
                        if(resourceExists(modelLocation))
                            textureLoc.addAll(collectJsonTexture(modelLocation));
                    }
                }
            }
            if(textureLoc.size() > 0)
                return textureLoc;
        } catch(FileNotFoundException ignored) {
        } catch(IOException | RuntimeException e) {
            VintageFix.LOGGER.error("Exception reading JSON for {}", jsonLocation, e);
        }
        return ImmutableList.of();
    }

    float lastIntegratedTickTime;
    @SubscribeEvent
    public void onTick(TickEvent.ServerTickEvent event) {
        if(FMLCommonHandler.instance().getSide().isClient() && event.side.isServer() && event.phase == TickEvent.Phase.END) {
            IntegratedServer srv = Minecraft.getMinecraft().getIntegratedServer();
            if(srv != null) {
                long currentTickTime = srv.tickTimeArray[srv.getTickCounter() % 100];
                lastIntegratedTickTime = lastIntegratedTickTime * 0.8F + (float)currentTickTime / 1000000.0F * 0.2F;
            } else
                lastIntegratedTickTime = 0;
        }
    }

    AllocationRateTracker tracker = new AllocationRateTracker();

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if(event.phase == TickEvent.Phase.START && !VintageFixCore.OPTIFINE)
            tracker.tick();
    }

    // highest to minimize number of entries that need shuffling
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRenderF3(RenderGameOverlayEvent.Text event) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if(!minecraft.gameSettings.showDebugInfo)
            return;
        IntegratedServer srv = minecraft.getIntegratedServer();
        if (srv != null) {
            event.getLeft().add(2, String.format("Integrated server @ %.0f ms ticks", lastIntegratedTickTime));
        }
        // only show when OptiFine isn't present, as it adds its own
        if(!VintageFixCore.OPTIFINE)
            event.getRight().add(2, String.format("Allocation rate: %03dMB /s", tracker.getAllocationRate()));
    }

    private static class AllocationRateTracker {
        private static final List<GarbageCollectorMXBean> COLLECTORS = ManagementFactory.getGarbageCollectorMXBeans();
        private static final long MINIMUM_TIME_AFTER_GC = 200;
        long prevFreeMem = Runtime.getRuntime().freeMemory();
        long timeBase = 0;
        long lastGcCount = -1;
        long allocationRate = 0;

        void tick() {
            long freeMem = Runtime.getRuntime().freeMemory();
            if(gcCountChanged() || freeMem > prevFreeMem) {
                prevFreeMem = freeMem;
                timeBase = System.nanoTime();
                return;
            }
            /* measure based on time */
            long milliDelta = (System.nanoTime() - timeBase) / (1000 * 1000);
            if(milliDelta <= MINIMUM_TIME_AFTER_GC)
                return;
            // bytes per millisecond
            long byteChange = prevFreeMem - freeMem;
            long allocationRateBytePerMilli = byteChange / milliDelta;
            // convert to MB per second
            allocationRate = allocationRateBytePerMilli * 1000L / (1000 * 1000);
        }

        boolean gcCountChanged() {
            long c = 0;
            for(GarbageCollectorMXBean collector : COLLECTORS) {
                c += collector.getCollectionCount();
            }
            boolean isSame = lastGcCount == c;
            lastGcCount = c;
            return !isSame;
        }

        long getAllocationRate() {
            return allocationRate;
        }
    }
}
