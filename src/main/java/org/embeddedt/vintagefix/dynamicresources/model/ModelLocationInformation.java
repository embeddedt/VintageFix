package org.embeddedt.vintagefix.dynamicresources.model;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelBakery;
import net.minecraft.client.renderer.block.model.ModelBlockDefinition;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.statemap.BlockStateMapper;
import net.minecraft.client.resources.IResource;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class ModelLocationInformation {
    public static final boolean DEBUG_MODEL_LOAD = Boolean.getBoolean("vintagefix.debugDynamicModelLoading");
    private static Map<Item, List<String>> variantNames;
    private static final Map<ModelResourceLocation, ResourceLocation> inventoryVariantLocations = new Object2ObjectOpenHashMap<>();
    private static final Map<ResourceLocation, Block> blockstateLocationToBlock = new Object2ObjectOpenHashMap<>();
    public static final Set<ModelResourceLocation> allItemVariants = new ObjectOpenHashSet<>();
    private static final Map<Block, Collection<ModelResourceLocation>> validVariantsForBlock = new Object2ObjectOpenHashMap<>();

    public static void init(ModelLoader loader, BlockStateMapper blockStateMapper) {
        Method method = ObfuscationReflectionHelper.findMethod(ModelBakery.class, "func_177592_e", Void.TYPE);
        try {
            method.invoke(loader);
            variantNames = ObfuscationReflectionHelper.getPrivateValue(ModelBakery.class, loader, "field_177613_u");
        } catch(ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }

        inventoryVariantLocations.clear();
        blockstateLocationToBlock.clear();
        allItemVariants.clear();
        validVariantsForBlock.clear();

        // Make inventory variant -> location map
        for (Item item : Item.REGISTRY) {
            for (String s : getVariantNames(item)) {
                ResourceLocation itemLocation = getItemLocation(s);
                ModelResourceLocation inventoryVariant = getInventoryVariant(s);
                allItemVariants.add(inventoryVariant);
                inventoryVariantLocations.put(inventoryVariant, itemLocation);
            }
        }

        // Make blockstate -> block map
        for (Block block : Block.REGISTRY) {
            for (ResourceLocation location : blockStateMapper.getBlockstateLocations(block)) {
                blockstateLocationToBlock.put(location, block);
            }
            ImmutableSet.Builder<ModelResourceLocation> variants = ImmutableSet.builder();
            for(ModelResourceLocation location : blockStateMapper.getVariants(block).values()) {
                ResourceLocation baseLocation = new ResourceLocation(location.getNamespace(), location.getPath());
                if(blockstateLocationToBlock.get(baseLocation) == block) {
                    variants.add(location);
                }
            }
            validVariantsForBlock.put(block, variants.build());
        }
    }

    public static ResourceLocation getInventoryVariantLocation(ModelResourceLocation inventoryVariant) {
        return inventoryVariantLocations.get(inventoryVariant);
    }

    public static void addInventoryVariantLocation(ModelResourceLocation inventoryVariant, ResourceLocation location) {
        inventoryVariantLocations.put(inventoryVariant, location);
    }

    public static boolean isAppropriateMultipart(Block block, ModelResourceLocation mrl) {
        Collection<ModelResourceLocation> collection = validVariantsForBlock.get(block);
        if(collection == null)
            return false;
        return collection.contains(mrl);
    }

    public static ResourceLocation getItemLocation(String location) {
        ResourceLocation resourcelocation = new ResourceLocation(location.replaceAll("#.*", ""));
        return new ResourceLocation(resourcelocation.getNamespace(), "item/" + resourcelocation.getPath());
    }

    public static ModelResourceLocation getInventoryVariant(String variant) {
        if (variant.contains("#")) {
            return new ModelResourceLocation(variant);
        }
        return new ModelResourceLocation(variant, "inventory");
    }

    public static List<String> getVariantNames(Item item) {
        List<String> list = variantNames.get(item);

        if (list == null) {
            list = Collections.singletonList(Item.REGISTRY.getNameForObject(item).toString());
        }

        return list;
    }

    public static Block getBlockFromBlockstateLocation(ResourceLocation blockstateLocation) {
        return blockstateLocationToBlock.get(blockstateLocation);
    }

    public static ModelBlockDefinition loadModelBlockDefinition(ResourceLocation location) {
        ResourceLocation blockstateLocation = new ResourceLocation(location.getNamespace(), "blockstates/" + location.getPath() + ".json");

        List<ModelBlockDefinition> list = Lists.newArrayList();
        try {
            for (IResource resource : Minecraft.getMinecraft().getResourceManager().getAllResources(blockstateLocation)) {
                list.add(loadModelBlockDefinition(location, resource));
            }
        } catch (IOException e) {
            throw new RuntimeException("Encountered an exception when loading model definition of model " + blockstateLocation, e);
        }

        return new ModelBlockDefinition(list);
    }

    private static ModelBlockDefinition loadModelBlockDefinition(ResourceLocation location, IResource resource) {
        ModelBlockDefinition definition;

        try (InputStream is = resource.getInputStream()) {
            definition = ModelBlockDefinition.parseFromReader(new InputStreamReader(is, StandardCharsets.UTF_8), location);
        } catch (Exception exception) {
            throw new RuntimeException("Encountered an exception when loading model definition of '" + location + "' from: '" + resource.getResourceLocation() + "' in resourcepack: '" + resource.getResourcePackName() + "'", exception);
        }

        return definition;
    }

}
