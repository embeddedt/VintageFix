package malte0811.ferritecore.mixin.dynamicresources.client;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import org.embeddedt.ferritecore.dynamicresources.model.BuiltinLoader;
import org.embeddedt.ferritecore.dynamicresources.model.DynamicModelProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(ModelLoaderRegistry.class)
public abstract class MixinModelLoaderRegistry {

    /** @reason Model loading and caching code has been moved to DynamicModelProvider. **/
    @Overwrite
    public static IModel getModel(ResourceLocation location) {
        return DynamicModelProvider.instance.getObject(location);
    }

    /** @reason Get the missing model from BuiltinLoader. **/
    @Overwrite
    public static IModel getMissingModel() {
        return BuiltinLoader.WRAPPED_MODEL_MISSING;
    }
}
