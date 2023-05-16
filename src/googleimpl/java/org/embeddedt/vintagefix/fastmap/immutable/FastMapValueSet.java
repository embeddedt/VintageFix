package org.embeddedt.vintagefix.fastmap.immutable;

import com.google.common.collect.FerriteCoreImmutableCollectionAccess;
import com.google.common.collect.UnmodifiableIterator;
import org.embeddedt.vintagefix.ducks.FastMapStateHolder;
import org.embeddedt.vintagefix.fastmap.FastMap;

import javax.annotation.Nullable;
import java.util.Objects;

public class FastMapValueSet extends FerriteCoreImmutableCollectionAccess<Comparable<?>> {
    private final FastMapStateHolder<?> viewedState;

    public FastMapValueSet(FastMapStateHolder<?> viewedState) {
        this.viewedState = viewedState;
    }

    @Override
    public UnmodifiableIterator<Comparable<?>> iterator() {
        return new FastMapEntryIterator<Comparable<?>>(viewedState) {
            @Override
            protected Comparable<?> getEntry(int propertyIndex, FastMap<?> map, int stateIndex) {
                return map.getKey(propertyIndex).getValue(stateIndex);
            }
        };
    }

    @Override
    public int size() {
        return viewedState.getStateMap().numProperties();
    }

    @Override
    public boolean contains(@Nullable Object o) {
        for (Comparable<?> entry : this) {
            if (Objects.equals(entry, o)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isPartialView() {
        return false;
    }
}
