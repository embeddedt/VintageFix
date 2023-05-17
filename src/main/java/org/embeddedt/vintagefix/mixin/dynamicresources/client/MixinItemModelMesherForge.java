package org.embeddedt.vintagefix.mixin.dynamicresources.client;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.renderer.ItemModelMesher;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelManager;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.client.ItemModelMesherForge;
import net.minecraftforge.registries.IRegistryDelegate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;

@Mixin(ItemModelMesherForge.class)
public class MixinItemModelMesherForge extends ItemModelMesher {
    @Shadow Map<IRegistryDelegate<Item>, Int2ObjectMap<ModelResourceLocation>> locations;

    public MixinItemModelMesherForge(ModelManager modelManager) {
        super(modelManager);
    }

    /**
     * @reason Get the stored location for that item and meta, and get the model
     * from that location from the model manager.
     **/
    @Overwrite
    @Override
    protected IBakedModel getItemModel(Item item, int meta) {
        Int2ObjectMap<ModelResourceLocation> map = locations.get(item.delegate);
        if(map == null)
            return null;
        ModelResourceLocation location = map.get(meta);
        if(location == null)
            return null;
        return getModelManager().getModel(location);
    }

    /**
     * @reason Don't get all models during init (with dynamic loading, that would
     * generate them all). Just store location instead.
     **/
    @Overwrite
    @Override
    public void register(Item item, int meta, ModelResourceLocation location) {
        IRegistryDelegate<Item> key = item.delegate;
        Int2ObjectMap<ModelResourceLocation> locs = locations.get(key);
        if (locs == null) {
            locs = new Int2ObjectOpenHashMap<>();
            locations.put(key, locs);
        }
        locs.put(meta, location);
    }

    /**
     * @reason Disable cache rebuilding (with dynamic loading, that would generate
     * all models).
     **/
    @Overwrite
    @Override
    public void rebuildCache() {}
}
