package org.embeddedt.vintagefix.mixin.blockstates;

import com.google.common.collect.ImmutableMap;
import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import org.embeddedt.vintagefix.ducks.FastMapStateHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = {"net/minecraftforge/common/property/ExtendedBlockState$ExtendedStateImplementation"})
public abstract class ExtendedStateImplementationMixin extends BlockStateContainer.StateImplementation {
    protected ExtendedStateImplementationMixin(Block blockIn, ImmutableMap<IProperty<?>, Comparable<?>> propertiesIn) {
        super(blockIn, propertiesIn);
    }

    @Shadow(remap = false)
    private IBlockState cleanState;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        IBlockState copyState = cleanState != null ? cleanState : getBlock().getBlockState().getBaseState();
        FastMapStateHolder<IBlockState> otherHolder = (FastMapStateHolder<IBlockState>)copyState;
        FastMapStateHolder<IBlockState> selfHolder = (FastMapStateHolder<IBlockState>)this;
        selfHolder.setStateMap(otherHolder.getStateMap());
        selfHolder.setStateIndex(otherHolder.getStateIndex());
    }
}
