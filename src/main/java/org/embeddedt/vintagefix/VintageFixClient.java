package org.embeddedt.vintagefix;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableListMultimap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.resources.FallbackResourceManager;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.SimpleReloadableResourceManager;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ColorHandlerEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.embeddedt.vintagefix.core.MixinConfigPlugin;
import org.embeddedt.vintagefix.core.VintageFixCore;
import org.embeddedt.vintagefix.dynamicresources.CTMHelper;
import org.embeddedt.vintagefix.dynamicresources.IWeakTextureMap;
import org.embeddedt.vintagefix.dynamicresources.ResourcePackHelper;
import org.embeddedt.vintagefix.impl.Deduplicator;
import org.embeddedt.vintagefix.util.Util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
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
    @SubscribeEvent
    public void registerListener(ColorHandlerEvent.Block event) {
        Deduplicator.registerReloadListener();
    }

    // target all textures in the listed subfolders, or textures in the root folder
    private static final Pattern TEXTURE_MATCH_PATTERN = Pattern.compile("^/?assets/(.+?(?=/))/textures/((?:(?:attachment|bettergrass|block.?|cape|crop.?|decors|item.?|entity/(armor|bed|chest)|fluid.?|model.?|part.?|pipe|rendering|ropebridge|solid_block|tile.?)/.*)|[A-Za-z0-9_\\-]*)\\.png$");

    private void registerSpriteSafe(TextureMap map, ResourceLocation location) {
        try {
            ((IWeakTextureMap)map).registerSpriteWeak(location);
        } catch(RuntimeException ignored) {
        }
    }

    private static final ImmutableListMultimap<String, ResourceLocation> EXTRA_TEXTURES_BY_MOD = ImmutableListMultimap.<String, ResourceLocation>builder()
        .put("mekanism", new ResourceLocation("mekanism", "entities/robit"))
        .build();

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void collectTextures(TextureStitchEvent.Pre event) {
        if(MixinConfigPlugin.isMixinClassApplied("mixin.dynamic_resources.TextureCollectionMixin")) {
            /* take every texture from these folders (1.19.3+ emulation) */
            Stopwatch watch = Stopwatch.createStarted();
            TextureMap map = event.getMap();
            Set<IResourcePack> resourcePacks = new LinkedHashSet<>();
            SimpleReloadableResourceManager manager = (SimpleReloadableResourceManager)Minecraft.getMinecraft().getResourceManager();
            Map<String, FallbackResourceManager> domainManagers = ObfuscationReflectionHelper.getPrivateValue(SimpleReloadableResourceManager.class, manager, "field_110548_a");
            for(FallbackResourceManager fallback : domainManagers.values()) {
                List<IResourcePack> fallbackPacks = ObfuscationReflectionHelper.getPrivateValue(FallbackResourceManager.class, fallback, "field_110540_a");
                resourcePacks.addAll(fallbackPacks);
            }
            int numFoundSprites = 0;
            for(IResourcePack pack : resourcePacks) {
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
            String[] gameFolders = new String[] { "resources", "oresources" };
            Path gameDirPath = Minecraft.getMinecraft().gameDir.toPath();
            for(String gameFolder : gameFolders) {
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
            watch.stop();
            VintageFix.LOGGER.info("Texture search took {}, total of {} collected sprites", watch, numFoundSprites);
        }
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
