package org.embeddedt.vintagefix.dynamicresources.model;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.block.model.ModelBakery;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.statemap.BlockStateMapper;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ModelLocationInformation {
    public static final boolean DEBUG_MODEL_LOAD = Boolean.getBoolean("vintagefix.debugDynamicModelLoading");
    private static Map<Item, List<String>> variantNames;
    private static final Map<ModelResourceLocation, ResourceLocation> inventoryVariantLocations = new Object2ObjectOpenHashMap<>();
    private static final Map<ResourceLocation, Block> blockstateLocationToBlock = new Object2ObjectOpenHashMap<>();

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

        // Make inventory variant -> location map
        for (Item item : Item.REGISTRY) {
            for (String s : getVariantNames(item)) {
                ResourceLocation itemLocation = getItemLocation(s);
                ModelResourceLocation inventoryVariant = getInventoryVariant(s);
                inventoryVariantLocations.put(inventoryVariant, itemLocation);
            }
        }

        // Make blockstate -> block map
        for (Block block : Block.REGISTRY) {
            for (ResourceLocation location : blockStateMapper.getBlockstateLocations(block)) {
                blockstateLocationToBlock.put(location, block);
            }
        }
    }

    public static ResourceLocation getInventoryVariantLocation(ModelResourceLocation inventoryVariant) {
        return inventoryVariantLocations.get(inventoryVariant);
    }

    public static void addInventoryVariantLocation(ModelResourceLocation inventoryVariant, ResourceLocation location) {
        inventoryVariantLocations.put(inventoryVariant, location);
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
}
