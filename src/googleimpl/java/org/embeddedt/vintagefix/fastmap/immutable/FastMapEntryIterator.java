package org.embeddedt.vintagefix.fastmap.immutable;

import com.google.common.collect.UnmodifiableIterator;
import org.embeddedt.vintagefix.ducks.FastMapStateHolder;
import org.embeddedt.vintagefix.fastmap.FastMap;

public abstract class FastMapEntryIterator<T> extends UnmodifiableIterator<T> {
    private final FastMapStateHolder<?> viewedState;
    private int currentIndex = 0;

    public FastMapEntryIterator(FastMapStateHolder<?> viewedState) {
        this.viewedState = viewedState;
    }

    @Override
    public boolean hasNext() {
        return currentIndex < viewedState.getStateMap().numProperties();
    }

    @Override
    public T next() {
        T next = getEntry(currentIndex, viewedState.getStateMap(), viewedState.getStateIndex());
        ++currentIndex;
        return next;
    }

    protected abstract T getEntry(int propertyIndex, FastMap<?> map, int stateIndex);
}
