package org.embeddedt.vintagefix.fastmap.table;

import com.google.common.collect.Table;
import net.minecraft.block.properties.IProperty;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class NeighborTableBase<S> implements Table<IProperty<?>, Comparable<?>, S> {
    protected static final String ISSUES_URL = "https://github.com/malte0811/FerriteCore/issues";

    @Override
    public void clear() {
        crashOnModify();
    }

    @Override
    public final S put(@Nonnull IProperty<?> rowKey, @Nonnull Comparable<?> columnKey, @Nonnull S value) {
        return crashOnModify();
    }

    @Override
    public final void putAll(@Nonnull Table<? extends IProperty<?>, ? extends Comparable<?>, ? extends S> table) {
        crashOnModify();
    }

    @Override
    public final S remove(@Nullable Object rowKey, @Nullable Object columnKey) {
        return crashOnModify();
    }

    private static <T> T crashOnModify() {
        throw new UnsupportedOperationException(
                "A mod tried to modify the state neighbor table directly. Please report this at " + ISSUES_URL
        );
    }
}
