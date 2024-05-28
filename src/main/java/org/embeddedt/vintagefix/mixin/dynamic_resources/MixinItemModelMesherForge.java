package org.embeddedt.vintagefix.mixin.dynamic_resources;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.renderer.ItemModelMesher;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelManager;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.client.ItemModelMesherForge;
import net.minecraftforge.registries.IRegistryDelegate;
import org.embeddedt.vintagefix.annotation.ClientOnlyMixin;
import org.embeddedt.vintagefix.dynamicresources.model.DynamicModelCache;
import org.spongepowered.asm.mixin.*;

import java.util.Map;

@Mixin(ItemModelMesherForge.class)
@ClientOnlyMixin
public class MixinItemModelMesherForge extends ItemModelMesher {
    @Shadow(remap = false) @Final @Mutable
    Map<IRegistryDelegate<Item>, Int2ObjectMap<ModelResourceLocation>> locations = new Object2ObjectOpenHashMap<>(512, 0.5F);

    // This is a pretty clever trick to speed up the model lookups - we know that our location objects per-item are unique,
    // so we can just do reference lookup on them
    private final DynamicModelCache<ModelResourceLocation> vintage$itemModelCache = new DynamicModelCache<>(this::getItemModelByLocationSlow, true);

    public MixinItemModelMesherForge(ModelManager modelManager) {
        super(modelManager);
    }


    private IBakedModel getItemModelByLocationSlow(ModelResourceLocation mrl) {
        return getModelManager().getModel(mrl);
    }

    /**
     * @author embeddedt, Runemoro
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
        return this.vintage$itemModelCache.get(location);
    }

    /**
     * @author embeddedt, Runemoro
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
     * @author embeddedt
     * @reason Disable cache rebuilding (with dynamic loading, that would generate
     * all models).
     **/
    @Overwrite
    @Override
    public void rebuildCache() {
        this.vintage$itemModelCache.clear();
    }
}
