package org.embeddedt.vintagefix.fastmap.immutable;

import com.google.common.collect.FerriteCoreEntrySetAccess;
import com.google.common.collect.UnmodifiableIterator;
import org.embeddedt.vintagefix.ducks.FastMapStateHolder;
import org.embeddedt.vintagefix.fastmap.FastMap;
import net.minecraft.block.properties.IProperty;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

public class FastMapEntryEntrySet extends FerriteCoreEntrySetAccess<IProperty<?>, Comparable<?>> {
    private final FastMapStateHolder<?> viewedState;

    public FastMapEntryEntrySet(FastMapStateHolder<?> viewedState) {
        this.viewedState = viewedState;
    }

    @Override
    @Nonnull
    public UnmodifiableIterator<Map.Entry<IProperty<?>, Comparable<?>>> iterator() {
        return new FastMapEntryIterator<Map.Entry<IProperty<?>, Comparable<?>>>(viewedState) {
            @Override
            protected Map.Entry<IProperty<?>, Comparable<?>> getEntry(
                    int propertyIndex, FastMap<?> map, int stateIndex
            ) {
                return map.getEntry(propertyIndex, stateIndex);
            }
        };
    }

    @Override
    public int size() {
        return viewedState.getStateMap().numProperties();
    }

    @Override
    public boolean contains(@Nullable Object object) {
        if (!(object instanceof Map.Entry<?, ?>)) {
            return false;
        }
        Comparable<?> valueInMap = viewedState.getStateMap().getValue(viewedState.getStateIndex(), ((Map.Entry<?, ?>)object).getKey());
        return valueInMap != null && valueInMap.equals(((Map.Entry<?, ?>) object).getValue());
    }

    @Override
    public boolean isPartialView() {
        return false;
    }
}
