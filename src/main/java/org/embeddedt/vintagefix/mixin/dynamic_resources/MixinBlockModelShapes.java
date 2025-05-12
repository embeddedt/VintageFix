package org.embeddedt.vintagefix.mixin.dynamic_resources;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.BlockModelShapes;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelManager;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.statemap.BlockStateMapper;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import org.embeddedt.vintagefix.annotation.ClientOnlyMixin;
import org.embeddedt.vintagefix.dynamicresources.IBlockModelShapes;
import org.embeddedt.vintagefix.dynamicresources.IModelHoldingBlockState;
import org.embeddedt.vintagefix.dynamicresources.StateMapperHandler;
import org.embeddedt.vintagefix.dynamicresources.model.DynamicBakedModelProvider;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;

@Mixin(BlockModelShapes.class)
@ClientOnlyMixin
public abstract class MixinBlockModelShapes implements IBlockModelShapes {
    @Shadow @Final private ModelManager modelManager;
    @Shadow @Final private BlockStateMapper blockStateMapper;

    @Shadow
    @Final
    @Mutable
    private Map<IBlockState, IBakedModel> bakedModelStore;

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
        for(Block block : ForgeRegistries.BLOCKS) {
            for(IBlockState state : block.getBlockState().getValidStates()) {
                ((IModelHoldingBlockState)state).vfix$setModel(null);
            }
        }
        this.bakedModelStore = DynamicBakedModelProvider.instance.getBakedModelStore();
    }

    private IBakedModel getModelForStateSlow(IBlockState state) {
        // Mods call this with null states
        if (state == null) {
            return modelManager.getMissingModel();
        }
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
        if(state instanceof IModelHoldingBlockState) {
            IModelHoldingBlockState holder = (IModelHoldingBlockState)state;
            IBakedModel model = holder.vfix$getModel();

            if(model != null) {
                return model;
            }

            model = this.getModelForStateSlow(state);
            holder.vfix$setModel(model);
            return model;
        } else {
            return this.getModelForStateSlow(state);
        }
    }

    @Override
    public ModelResourceLocation getLocationForState(IBlockState state) {
        return stateMapperHandler.getModelLocationForState(state);
    }
}
