package org.embeddedt.vintagefix.dynamicresources;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableSet;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fml.common.ProgressManager;
import org.apache.commons.io.IOUtils;
import org.embeddedt.vintagefix.VintageFix;
import org.embeddedt.vintagefix.VintageFixClient;
import org.embeddedt.vintagefix.dynamicresources.model.ModelLocationInformation;
import org.embeddedt.vintagefix.util.Util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinTask;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Implements a variation of the modern logic used by 1.19.3+ to load textures - scan resource packs for their given
 * folders.
 */
public class TextureCollector {

    // target all textures in the listed subfolders, or textures in the root folder
    private static final Pattern TEXTURE_MATCH_PATTERN = Pattern.compile("^/?assets/(.+?(?=/))/textures/((?:attachment|aspect.?|bettergrass|block.?|cape|customoverlay|decors|item.?|entity/(armor|bed|chest)|fluid.?|model.?|part.?|pipe|rendering|ropebridge|slot.?|solid_block|tile.?|tinkers|tconstruct|valuetype.?)/.*)\\.png$");

    private static final ImmutableListMultimap<String, ResourceLocation> EXTRA_TEXTURES_BY_MOD = ImmutableListMultimap.<String, ResourceLocation>builder()
        .put("mekanism", new ResourceLocation("mekanism", "entities/robit"))
        .put("gbook", new ResourceLocation("gbook", "cover"))
        .put("gbook", new ResourceLocation("gbook", "cover_gray"))
        .put("gbook", new ResourceLocation("gbook", "paper"))
        .put("gbook", new ResourceLocation("gbook", "transparent"))
        .put("industrialwires", new ResourceLocation("minecraft", "font/ascii"))
        .build();

    List<IResourcePack> resourcePackList;

    public static Set<String> weaklyCollectedTextures = ImmutableSet.of();

    TextureCollector() {
        resourcePackList = VintageFixClient.getResourcePackList();
    }

    public static void startDiscovery() {
        TextureCollector collector = new TextureCollector();
        VintageFixClient.discoveredTextures = VintageFix.WORKER_POOL.submit(collector::getAllTextureLocations);
    }

    Set<ResourceLocation> getAllTextureLocations() {
        /* take every texture from these folders (1.19.3+ emulation) */
        Set<ResourceLocation> allTextures = new HashSet<>();
        Stopwatch watch = Stopwatch.createStarted();
        ForkJoinTask<List<ResourceLocation>> modelTextures = VintageFix.WORKER_POOL.submit(this::collectModelTextures);
        int numFoundSprites = 0;
        String[] gameFolders = new String[] { "resources", "oresources" };
        for(IResourcePack pack : resourcePackList) {
            try {
                Collection<String> paths = ResourcePackHelper.getAllPaths(pack, s -> true);
                for(String path : paths) {
                    Matcher matcher = TEXTURE_MATCH_PATTERN.matcher(path);
                    if(matcher.matches()) {
                        allTextures.add(new ResourceLocation(matcher.group(1), matcher.group(2)));
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
            Path base = gameDirPath.resolve(gameFolder);
            try(Stream<Path> stream = Files.walk(base)) {
                Iterator<String> iterator = stream.map(base::relativize).map(path -> "assets/" + Util.normalizePathToString(path)).iterator();
                while(iterator.hasNext()) {
                    String p = iterator.next();
                    Matcher matcher = TEXTURE_MATCH_PATTERN.matcher(p);
                    if(matcher.matches()) {
                        allTextures.add(new ResourceLocation(matcher.group(1), matcher.group(2)));
                        numFoundSprites++;
                    }
                }
            } catch(FileNotFoundException | NoSuchFileException ignored) {
            } catch(IOException e) {
                VintageFix.LOGGER.error("Error listing resources", e);
            }
        }

        for(Map.Entry<String, ResourceLocation> entry : EXTRA_TEXTURES_BY_MOD.entries()) {
            allTextures.add(entry.getValue());
            numFoundSprites++;
        }
        for(ResourceLocation location : modelTextures.join()) {
            allTextures.add(location);
            numFoundSprites++;
        }
        watch.stop();
        VintageFix.LOGGER.info("Texture search took {}, total of {} collected sprites", watch, numFoundSprites);
        return allTextures;
    }


    Set<ResourceLocation> lookedAtLocations = Collections.synchronizedSet(new ObjectOpenHashSet<>());
    Map<ResourceLocation, Boolean> doesResourceExist = new ConcurrentHashMap<>();

    private boolean resourceExists(ResourceLocation loc) {
        return doesResourceExist.computeIfAbsent(loc, rl -> {
            try(IResource ignored1 = Minecraft.getMinecraft().getResourceManager().getResource(rl)) {
                return true;
            } catch(IOException ignored) {
            }
            return false;
        });
    }

    private List<ResourceLocation> collectModelTextures() {
        // wait for model locations to be available
        ModelLocationInformation.initFuture.join();
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

    private List<ResourceLocation> collectJsonTexture(ResourceLocation jsonLocation) {
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
        } catch(Throwable e) {
            VintageFix.LOGGER.error("Exception reading JSON for {}", jsonLocation, e);
        }
        return ImmutableList.of();
    }

}
