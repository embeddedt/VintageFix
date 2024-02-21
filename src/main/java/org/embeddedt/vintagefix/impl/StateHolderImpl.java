package org.embeddedt.vintagefix.impl;

import com.google.common.collect.ImmutableTable;
import org.embeddedt.vintagefix.classloading.FastImmutableMapDefiner;
import org.embeddedt.vintagefix.ducks.FastMapStateHolder;
import org.embeddedt.vintagefix.fastmap.FastMap;
import net.minecraft.block.properties.IProperty;

import java.util.Map;

public class StateHolderImpl {
    public static final ThreadLocal<Map<Map<IProperty<?>, Comparable<?>>, ?>> LAST_STATE_MAP = new ThreadLocal<>();
    public static final ThreadLocal<FastMap<?>> LAST_FAST_STATE_MAP = new ThreadLocal<>();

    /**
     * Set up the {@link FastMap} used by the given {@link FastMapStateHolder} to handle neighbors and property lookups.
     * This is called in a loop for each {@link net.minecraft.block.state.BlockStateContainer}, so all state holders of a given
     * container will use the same {@link FastMap} instance.
     */
    public static <S>
    void populateNeighbors(Map<Map<IProperty<?>, Comparable<?>>, S> states, FastMapStateHolder<S> holder) {
        if(states.size() == 1) {
            // Only one state => (try)setValue will never be successful, so we do not need to populate the FastMap as it
            // can never be queried. Additionally, the state map is already initialized to an empty "official"
            // ImmutableMap, which is a singleton and as such does not need to be replaced. Instead, we just initialize
            // the neighbor table as a singleton empty table as there are no neighbor blockstates.
            holder.setNeighborTable(ImmutableTable.of());
            return;
        }
        if (holder.getNeighborTable() != null) {
            throw new IllegalStateException();
        } else if (states == LAST_STATE_MAP.get()) {
            // Use threadlocal state to use the same fast map for all states of one block
            holder.setStateMap((FastMap<S>) LAST_FAST_STATE_MAP.get());
        } else {
            LAST_STATE_MAP.set(states);
            FastMap<S> globalTable = new FastMap<>(
                    holder.getVanillaPropertyMap().keySet(), states, false //FerriteConfig.COMPACT_FAST_MAP.isEnabled()
            );
            holder.setStateMap(globalTable);
            LAST_FAST_STATE_MAP.set(globalTable);
        }
        int index = holder.getStateMap().getIndexOf(holder.getVanillaPropertyMap());
        holder.setStateIndex(index);
        if (true) { //FerriteConfig.PROPERTY_MAP.isEnabled()) {
            holder.replacePropertyMap(FastImmutableMapDefiner.makeMap(holder));
        }
        /* TODO: figure out how to fake an ImmutableTable on 1.12 */
        holder.setNeighborTable(null);
    }

    public static void clearCachedStateMaps() {
        LAST_STATE_MAP.remove();
        LAST_FAST_STATE_MAP.remove();
    }
}
