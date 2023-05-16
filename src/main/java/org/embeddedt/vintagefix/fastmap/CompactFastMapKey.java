package org.embeddedt.vintagefix.fastmap;

import net.minecraft.block.properties.IProperty;

/**
 * A "compact" implementation of a FastMapKey, i.e. one which completely fills the value matrix
 */
public class CompactFastMapKey<T extends Comparable<T>> extends FastMapKey<T> {
    private final int mapFactor;

    CompactFastMapKey(IProperty<T> property, int mapFactor) {
        super(property);
        this.mapFactor = mapFactor;
    }

    @Override
    public T getValue(int mapIndex) {
        int index = (mapIndex / mapFactor) % numValues();
        return byInternalIndex(index);
    }

    @Override
    public int replaceIn(int mapIndex, T newValue) {
        final int lowerData = mapIndex % mapFactor;
        final int upperFactor = mapFactor * numValues();
        final int upperData = mapIndex - mapIndex % upperFactor;
        int internalIndex = getInternalIndex(newValue);
        if (internalIndex < 0 || internalIndex >= numValues()) {
            return -1;
        } else {
            return lowerData + mapFactor * internalIndex + upperData;
        }
    }

    @Override
    public int toPartialMapIndex(Comparable<?> value) {
        return mapFactor * getInternalIndex(value);
    }

    @Override
    public int getFactorToNext() {
        return numValues();
    }
}
