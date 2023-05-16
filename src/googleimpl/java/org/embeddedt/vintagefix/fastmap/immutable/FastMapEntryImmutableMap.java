package org.embeddedt.vintagefix.fastmap.immutable;

import com.google.common.collect.FerriteCoreImmutableMapAccess;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableSet;
import org.embeddedt.vintagefix.ducks.FastMapStateHolder;
import net.minecraft.block.properties.IProperty;

import javax.annotation.Nullable;

public class FastMapEntryImmutableMap extends FerriteCoreImmutableMapAccess<IProperty<?>, Comparable<?>> {
    private final FastMapStateHolder<?> viewedState;

    public FastMapEntryImmutableMap(FastMapStateHolder<?> viewedState) {
        this.viewedState = viewedState;
    }

    @Override
    public int size() {
        return viewedState.getStateMap().numProperties();
    }

    @Override
    public Comparable<?> get(@Nullable Object key) {
        return viewedState.getStateMap().getValue(viewedState.getStateIndex(), key);
    }

    @Override
    public ImmutableSet<Entry<IProperty<?>, Comparable<?>>> createEntrySet() {
        return new FastMapEntryEntrySet(viewedState);
    }

    @Override
    public ImmutableSet<Entry<IProperty<?>, Comparable<?>>> entrySet() {
        return new FastMapEntryEntrySet(viewedState);
    }

    @Override
    public boolean isPartialView() {
        return false;
    }

    @Override
    public ImmutableSet<IProperty<?>> createKeySet() {
        return viewedState.getStateMap().getPropertySet();
    }

    @Override
    public ImmutableCollection<Comparable<?>> createValues() {
        return new FastMapValueSet(viewedState);
    }
}
