package org.embeddedt.vintagefix;

import com.google.common.collect.ImmutableList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.resources.*;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
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
import org.embeddedt.vintagefix.impl.Deduplicator;
import org.embeddedt.vintagefix.transformercache.TransformerCache;
import org.embeddedt.vintagefix.util.VersionProtester;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.Locale;
import java.util.*;
import java.util.concurrent.ForkJoinTask;

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

    private void registerSpriteSafe(TextureMap map, ResourceLocation location) {
        try {
            ((IWeakTextureMap)map).registerSpriteWeak(location);
        } catch(RuntimeException ignored) {
        }
    }

    private void registerSpriteIfPresent(TextureMap map, ResourceLocation location) {
        boolean present = false;
        try(IResource ignored1 = Minecraft.getMinecraft().getResourceManager().getResource(new ResourceLocation(location.getNamespace(), "textures/" + location.getPath() + ".png"))) {
            present = true;
        } catch(Exception ignored) {}
        if(present)
            registerSpriteSafe(map, location);
    }

    public static List<IResourcePack> getResourcePackList() {
        Set<IResourcePack> resourcePacks = new LinkedHashSet<>();
        SimpleReloadableResourceManager manager = (SimpleReloadableResourceManager)Minecraft.getMinecraft().getResourceManager();
        Map<String, FallbackResourceManager> domainManagers = ObfuscationReflectionHelper.getPrivateValue(SimpleReloadableResourceManager.class, manager, "field_110548_a");
        for(FallbackResourceManager fallback : domainManagers.values()) {
            List<IResourcePack> fallbackPacks = ObfuscationReflectionHelper.getPrivateValue(FallbackResourceManager.class, fallback, "field_110540_a");
            resourcePacks.addAll(fallbackPacks);
        }
        return new ArrayList<>(resourcePacks);
    }

    public static ForkJoinTask<Set<ResourceLocation>> discoveredTextures = null;

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void collectTextures(TextureStitchEvent.Pre event) {
        if(MixinConfigPlugin.isMixinClassApplied("mixin.dynamic_resources.TextureCollectionMixin")) {
            Objects.requireNonNull(discoveredTextures, "Future not found");
            Set<ResourceLocation> allTextures = discoveredTextures.join();
            TextureMap map = event.getMap();
            for(ResourceLocation tex : allTextures) {
                registerSpriteSafe(map, tex);
            }
            // register all fluid textures
            for(Fluid f : FluidRegistry.getRegisteredFluids().values()) {
                if(f.getStill() != null)
                    registerSpriteIfPresent(map, f.getStill());
                if(f.getFlowing() != null)
                    registerSpriteIfPresent(map, f.getFlowing());
            }
            discoveredTextures = null;
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

    private static boolean protestVersion = MixinConfigPlugin.isMixinClassApplied("mixin.version_protest.F3Change");

    // highest to minimize number of entries that need shuffling
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRenderF3(RenderGameOverlayEvent.Text event) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if(!minecraft.gameSettings.showDebugInfo || event.getLeft().size() < 2 || event.getRight().size() < 2)
            return;
        IntegratedServer srv = minecraft.getIntegratedServer();
        if (srv != null) {
            event.getLeft().add(2, String.format("Integrated server @ %.0f ms ticks", lastIntegratedTickTime));
        }
        // only show when OptiFine isn't present, as it adds its own
        if(!VintageFixCore.OPTIFINE)
            event.getRight().add(2, String.format("Allocation rate: %03dMB /s", tracker.getAllocationRate()));
        if(protestVersion) {
            event.getLeft().set(0, VersionProtester.protest(event.getLeft().get(0)));
        }
    }

    private static float gameStartTime = -1;

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onGuiOpen(GuiOpenEvent event) {
        if(!event.isCanceled() && event.getGui() instanceof GuiMainMenu && gameStartTime == -1) {
            gameStartTime = ManagementFactory.getRuntimeMXBean().getUptime() / 1000f;
            VintageFix.LOGGER.info("Game launch took " + gameStartTime + " seconds");
            TransformerCache.instance.printStats();
        }
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
