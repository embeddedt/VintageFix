package org.embeddedt.vintagefix.mixin.blockstates;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableTable;
import net.minecraft.block.Block;
import org.embeddedt.vintagefix.ducks.FastMapStateHolder;
import org.embeddedt.vintagefix.fastmap.FastMap;
import org.embeddedt.vintagefix.impl.StateHolderImpl;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Map;

@Mixin(BlockStateContainer.StateImplementation.class)
public abstract class FastMapStateHolderMixin implements FastMapStateHolder<IBlockState> {
    @Mutable
    @Shadow
    @Final
    private ImmutableMap<IProperty<?>, Comparable<?>> properties;
    @Shadow
    private ImmutableTable<IProperty<?>, Comparable<?>, IBlockState> propertyValueTable;

    @Shadow
    @Final
    private Block block;
    private int ferritecore_globalTableIndex;
    private FastMap<IBlockState> ferritecore_globalTable;

    @Redirect(
            method = "withProperty",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/google/common/collect/ImmutableTable;get(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;",
                    remap = false
            )
    )
    public Object getNeighborFromFastMap(ImmutableTable<?, ?, ?> ignore, Object rowKey, Object columnKey) {
        try {
            return this.ferritecore_globalTable.withUnsafe(
                this.ferritecore_globalTableIndex,
                (IProperty<?>) rowKey,
                columnKey
            );
        } catch(RuntimeException e) {
            // To match vanilla behavior, wrap this in an IllegalArgumentException, not any other type of exception
            throw new IllegalArgumentException("Cannot set property " + rowKey + " to " + columnKey + " on block " + Block.REGISTRY.getNameForObject(this.block), e);
        }
    }

    /**
     * @reason This Mixin completely replaces the data structures initialized by this method, as the original ones waste
     * a lot of memory
     * @author malte0811
     */
    @Overwrite
    public void buildPropertyValueTable(Map<Map<IProperty<?>, Comparable<?>>, IBlockState> states) {
        StateHolderImpl.populateNeighbors(states, this);
    }

    @Override
    public FastMap<IBlockState> getStateMap() {
        return ferritecore_globalTable;
    }

    @Override
    public int getStateIndex() {
        return ferritecore_globalTableIndex;
    }

    @Override
    public ImmutableMap<IProperty<?>, Comparable<?>> getVanillaPropertyMap() {
        return properties;
    }

    @Override
    public void replacePropertyMap(ImmutableMap<IProperty<?>, Comparable<?>> newMap) {
        properties = newMap;
    }

    @Override
    public void setStateMap(FastMap<IBlockState> newValue) {
        ferritecore_globalTable = newValue;
    }

    @Override
    public void setStateIndex(int newValue) {
        ferritecore_globalTableIndex = newValue;
    }

    @Override
    public void setNeighborTable(ImmutableTable<IProperty<?>, Comparable<?>, IBlockState> table) {
        propertyValueTable = table;
    }

    @Override
    public ImmutableTable<IProperty<?>, Comparable<?>, IBlockState> getNeighborTable() {
        return propertyValueTable;
    }

    /**
     * @author embeddedt
     * @reason states are unique, no need to waste time hashing the map
     */
    @Overwrite
    public int hashCode() {
        return System.identityHashCode(this);
    }
}
