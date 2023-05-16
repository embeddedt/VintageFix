package org.embeddedt.vintagefix.fastmap.table;

import net.minecraft.block.properties.IProperty;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Singleton, which is assigned as the neighbor table for all blockstates by default. This makes it clear who is to be
 * blamed for any crashes, and also how to work around them.
 */
public class CrashNeighborTable<S> extends NeighborTableBase<S> {
    private static final CrashNeighborTable<?> INSTANCE = new CrashNeighborTable<>();

    @SuppressWarnings("unchecked")
    public static <S> CrashNeighborTable<S> getInstance() {
        return (CrashNeighborTable<S>) INSTANCE;
    }

    private CrashNeighborTable() {}

    @Override
    public boolean contains(@Nullable Object rowKey, @Nullable Object columnKey) {
        return crashOnAccess();
    }

    @Override
    public boolean containsRow(@Nullable Object rowKey) {
        return crashOnAccess();
    }

    @Override
    public boolean containsColumn(@Nullable Object columnKey) {
        return crashOnAccess();
    }

    @Override
    public boolean containsValue(@Nullable Object value) {
        return crashOnAccess();
    }

    @Override
    public S get(@Nullable Object rowKey, @Nullable Object columnKey) {
        return crashOnAccess();
    }

    @Override
    public boolean isEmpty() {
        return crashOnAccess();
    }

    @Override
    public int size() {
        return crashOnAccess();
    }

    @Override
    public Map<Comparable<?>, S> row(@Nonnull IProperty<?> rowKey) {
        return crashOnAccess();
    }

    @Override
    public Map<IProperty<?>, S> column(@Nonnull Comparable<?> columnKey) {
        return crashOnAccess();
    }

    @Override
    public Set<Cell<IProperty<?>, Comparable<?>, S>> cellSet() {
        return crashOnAccess();
    }

    @Override
    public Set<IProperty<?>> rowKeySet() {
        return crashOnAccess();
    }

    @Override
    public Set<Comparable<?>> columnKeySet() {
        return crashOnAccess();
    }

    @Override
    public Collection<S> values() {
        return crashOnAccess();
    }

    @Override
    public Map<IProperty<?>, Map<Comparable<?>, S>> rowMap() {
        return crashOnAccess();
    }

    @Override
    public Map<Comparable<?>, Map<IProperty<?>, S>> columnMap() {
        return crashOnAccess();
    }

    private static <T> T crashOnAccess() {
        throw new UnsupportedOperationException(
                "A mod tried to access the state neighbor table directly. Please report this at " + ISSUES_URL +
                        ". As a temporary workaround you can enable \"populateNeighborTable\" in the FerriteCore config"
        );
    }
}
