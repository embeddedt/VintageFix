package org.embeddedt.vintagefix.fastmap;

import net.minecraft.block.properties.IProperty;

import java.util.*;

/**
 * Fake "map" implementation used to hold the states.
 *
 * Intentionally throws on methods that would be inefficient so that we know
 * if an incompatible mod is present.
 */
public class FakeStateMap<S> implements Map<Map<IProperty<?>, Comparable<?>>, S> {
    private final Map<IProperty<?>, Comparable<?>>[] keys;
    private final Object[] values;
    private int usedSlots;
    public FakeStateMap(int numStates) {
        this.keys = new Map[numStates];
        this.values = new Object[numStates];
        this.usedSlots = 0;
    }

    @Override
    public int size() {
        return usedSlots;
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public boolean containsKey(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsValue(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public S get(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public S put(Map<IProperty<?>, Comparable<?>> propertyComparableMap, S s) {
        keys[usedSlots] = propertyComparableMap;
        values[usedSlots] = s;
        usedSlots++;
        return null;
    }

    @Override
    public S remove(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void putAll(Map<? extends Map<IProperty<?>, Comparable<?>>, ? extends S> map) {
        for(Entry<? extends Map<IProperty<?>, Comparable<?>>, ? extends S> entry : map.entrySet()) {
            this.put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void clear() {
        for(int i = 0; i < this.keys.length; i++) {
            this.keys[i] = null;
            this.values[i] = null;
        }
        this.usedSlots = 0;
    }

    @Override
    public Set<Map<IProperty<?>, Comparable<?>>> keySet() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<S> values() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<Entry<Map<IProperty<?>, Comparable<?>>, S>> entrySet() {
        return new Set<Entry<Map<IProperty<?>, Comparable<?>>, S>>() {
            @Override
            public int size() {
                return usedSlots;
            }

            @Override
            public boolean isEmpty() {
                return FakeStateMap.this.isEmpty();
            }

            @Override
            public boolean contains(Object o) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Iterator<Entry<Map<IProperty<?>, Comparable<?>>, S>> iterator() {
                return new Iterator<Entry<Map<IProperty<?>, Comparable<?>>, S>>() {
                    int currentIdx = 0;
                    @Override
                    public boolean hasNext() {
                        return currentIdx < usedSlots;
                    }

                    @Override
                    public Entry<Map<IProperty<?>, Comparable<?>>, S> next() {
                        if(currentIdx >= usedSlots)
                            throw new IndexOutOfBoundsException();
                        Entry<Map<IProperty<?>, Comparable<?>>, S> entry = new AbstractMap.SimpleImmutableEntry<>(keys[currentIdx], (S)values[currentIdx]);
                        currentIdx++;
                        return entry;
                    }
                };
            }

            @Override
            public Object[] toArray() {
                throw new UnsupportedOperationException();
            }

            @Override
            public <T> T[] toArray(T[] ts) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean add(Entry<Map<IProperty<?>, Comparable<?>>, S> mapSEntry) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean remove(Object o) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean containsAll(Collection<?> collection) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean addAll(Collection<? extends Entry<Map<IProperty<?>, Comparable<?>>, S>> collection) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean retainAll(Collection<?> collection) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean removeAll(Collection<?> collection) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void clear() {
                throw new UnsupportedOperationException();
            }
        };
    }
}
