package org.embeddedt.vintagefix;

import com.google.common.base.Stopwatch;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.resources.FallbackResourceManager;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.SimpleReloadableResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ColorHandlerEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.embeddedt.vintagefix.dynamicresources.CTMHelper;
import org.embeddedt.vintagefix.dynamicresources.ResourcePackHelper;
import org.embeddedt.vintagefix.impl.Deduplicator;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class VintageFixClient {
    public VintageFixClient() {
        if(Loader.isModLoaded("ctm")) {
            MinecraftForge.EVENT_BUS.register(CTMHelper.class);
        }
    }
    @SubscribeEvent
    public void registerListener(ColorHandlerEvent.Block event) {
        Deduplicator.registerReloadListener();
    }

    private static final Pattern TEXTURE_MATCH_PATTERN = Pattern.compile("^/?assets/(.+?(?=/))/textures/((?:attachment|bettergrass|block.?|cape|item.?|entity/(bed|chest)|model.?|part.?|pipe|ropebridge)/.*)\\.png$");

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void collectTextures(TextureStitchEvent.Pre event) {
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
        for(IResourcePack pack : resourcePacks) {
            try {
                Collection<String> paths = ResourcePackHelper.getAllPaths(pack, s -> true);
                List<ResourceLocation> locations = paths.stream().flatMap(str -> {
                    Matcher matcher = TEXTURE_MATCH_PATTERN.matcher(str);
                    if(matcher.matches()) {
                        return Stream.of(new ResourceLocation(matcher.group(1), matcher.group(2)));
                    } else
                        return Stream.of();
                }).collect(Collectors.toList());
                for(ResourceLocation texLoc : locations) {
                    map.registerSprite(texLoc);
                }
            } catch(IOException e) {
                VintageFix.LOGGER.error("Error listing resources", e);
            }
        }
        watch.stop();
        VintageFix.LOGGER.info("Texture search took {}", watch);
    }

}
