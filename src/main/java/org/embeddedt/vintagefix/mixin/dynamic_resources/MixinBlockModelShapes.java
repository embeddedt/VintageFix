package org.embeddedt.vintagefix.mixin.dynamic_resources;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.BlockModelShapes;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelManager;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.statemap.BlockStateMapper;
import org.embeddedt.vintagefix.annotation.ClientOnlyMixin;
import org.embeddedt.vintagefix.dynamicresources.IBlockModelShapes;
import org.embeddedt.vintagefix.dynamicresources.StateMapperHandler;
import org.embeddedt.vintagefix.dynamicresources.model.DynamicModelCache;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;
import java.util.concurrent.ExecutionException;

@Mixin(BlockModelShapes.class)
@ClientOnlyMixin
public abstract class MixinBlockModelShapes implements IBlockModelShapes {
    @Shadow @Final private ModelManager modelManager;
    @Shadow @Final private BlockStateMapper blockStateMapper;

    private final DynamicModelCache<IBlockState> vintage$modelCache = new DynamicModelCache<>(this::getModelForStateSlow, false);

    private static StateMapperHandler stateMapperHandler;

    /**
     * @author embeddedt, Runemoro
     * @reason Don't get all models during init (with dynamic loading, that would
     * generate them all). Just store location instead.
     **/
    @Overwrite
    public void reloadModels() {
        if(stateMapperHandler == null) {
            stateMapperHandler = new StateMapperHandler(this.blockStateMapper);
        }
        this.vintage$modelCache.clear();
    }

    private IBakedModel getModelForStateSlow(IBlockState state) {
        IBakedModel model = modelManager.getModel(getLocationForState(state));
        if (model == null) {
            model = modelManager.getMissingModel();
        }
        return model;
    }

    /**
     * @author embeddedt, Runemoro
     * @reason Get the stored location for that state, and get the model from
     * that location from the model manager.
     **/
    @Overwrite
    public IBakedModel getModelForState(IBlockState state) {
        return this.vintage$modelCache.get(state);
    }

    @Override
    public ModelResourceLocation getLocationForState(IBlockState state) {
        return stateMapperHandler.getModelLocationForState(state);
    }
}
