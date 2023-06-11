package org.embeddedt.vintagefix.fastmap;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenCustomHashMap;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.IStringSerializable;
import org.embeddedt.vintagefix.VintageFix;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Provides a way of converting between values of a property and indices in [0, #values). Most properties are covered
 * by one of the (faster) specific implementations, all other properties use the {@link GenericIndexer}
 */
public abstract class PropertyIndexer<T extends Comparable<T>> {
    private static final Map<IProperty<?>, PropertyIndexer<?>> KNOWN_INDEXERS = new Object2ObjectOpenCustomHashMap<>(new Hash.Strategy<IProperty<?>>() {
        @Override
        public int hashCode(IProperty<?> o) {
            return System.identityHashCode(o);
        }

        @Override
        public boolean equals(IProperty<?> a, IProperty<?> b) {
            return a == b;
        }
    });

    private final IProperty<T> property;
    private final int numValues;
    protected final T[] valuesInOrder;

    public static <T extends Comparable<T>> PropertyIndexer<T> makeIndexer(IProperty<T> prop) {
        synchronized (KNOWN_INDEXERS) {
            PropertyIndexer<?> unchecked = KNOWN_INDEXERS.computeIfAbsent(prop, propInner -> {
                PropertyIndexer<?> result = null;
                if (propInner instanceof PropertyBool) {
                    result = new BoolIndexer((PropertyBool)propInner);
                } else if (propInner instanceof PropertyInteger) {
                    result = new IntIndexer((PropertyInteger)propInner);
                } else if (WeirdVanillaDirectionIndexer.isApplicable(propInner)) {
                    result = new WeirdVanillaDirectionIndexer((IProperty<EnumFacing>)propInner);
                } else if (propInner instanceof PropertyEnum<?>) {
                    result = new EnumIndexer<>((PropertyEnum<?>)propInner);
                }
                if (result == null || !result.isValid()) {
                    return new GenericIndexer<>(propInner);
                } else {
                    return result;
                }
            });
            return (PropertyIndexer<T>) unchecked;
        }
    }

    protected PropertyIndexer(IProperty<T> property, T[] valuesInOrder) {
        this.property = property;
        this.numValues = property.getAllowedValues().size();
        this.valuesInOrder = valuesInOrder;
    }

    public IProperty<T> getProperty() {
        return property;
    }

    public int numValues() {
        return numValues;
    }

    @Nullable
    public final T byIndex(int index) {
        if (index >= 0 && index < valuesInOrder.length) {
            return valuesInOrder[index];
        } else {
            return null;
        }
    }

    public abstract int toIndex(T value);

    /**
     * Checks if this indexer is valid, i.e. iterates over the correct set of values in the correct order
     */
    protected boolean isValid() {
        Collection<T> allowed = getProperty().getAllowedValues();
        int index = 0;
        for (T val : allowed) {
            if (toIndex(val) != index || !val.equals(byIndex(index))) {
                return false;
            }
            ++index;
        }
        return true;
    }

    private static class BoolIndexer extends PropertyIndexer<Boolean> {
        private static final Boolean[] VALUES = {true, false};

        protected BoolIndexer(PropertyBool property) {
            super(property, VALUES);
        }

        @Override
        public int toIndex(Boolean value) {
            return value ? 0 : 1;
        }
    }

    private static class IntIndexer extends PropertyIndexer<Integer> {
        private final int min;

        protected IntIndexer(PropertyInteger property) {
            super(property, property.getAllowedValues().toArray(new Integer[0]));
            this.min = property.getAllowedValues().stream().min(Comparator.naturalOrder()).orElse(0);
        }

        @Override
        public int toIndex(Integer value) {
            return value - min;
        }
    }

    private static class EnumIndexer<E extends Enum<E> & IStringSerializable>
            extends PropertyIndexer<E> {
        private final int ordinalOffset;

        protected EnumIndexer(PropertyEnum<E> property) {
            super(property, property.getAllowedValues().toArray((E[]) new Enum<?>[0]));
            this.ordinalOffset = property.getAllowedValues()
                    .stream()
                    .mapToInt(Enum::ordinal)
                    .min()
                    .orElse(0);
        }

        @Override
        public int toIndex(E value) {
            return value.ordinal() - ordinalOffset;
        }
    }

    /**
     * This is a kind of hack for a vanilla quirk: BlockStateProperties.FACING (which is used everywhere) has the order
     * NORTH, EAST, SOUTH, WEST, UP, DOWN
     * instead of the "canonical" order given by the enum
     */
    private static class WeirdVanillaDirectionIndexer extends PropertyIndexer<EnumFacing> {
        private static final EnumFacing[] ORDER = {
            EnumFacing.NORTH, EnumFacing.EAST, EnumFacing.SOUTH, EnumFacing.WEST, EnumFacing.UP, EnumFacing.DOWN
        };

        public WeirdVanillaDirectionIndexer(IProperty<EnumFacing> prop) {
            super(prop, ORDER);
            Preconditions.checkState(isValid());
        }

        static boolean isApplicable(IProperty<?> prop) {
            Collection<?> values = prop.getAllowedValues();
            if (values.size() != ORDER.length) {
                return false;
            }
            return Arrays.equals(ORDER, values.toArray());
        }

        @Override
        public int toIndex(EnumFacing value) {
            switch (value) {
                case NORTH: return 0;
                case EAST: return 1;
                case SOUTH: return 2;
                case WEST: return 3;
                case UP: return 4;
                case DOWN: default: return 5;
            }
        }
    }

    private static class GenericIndexer<T extends Comparable<T>> extends PropertyIndexer<T> {
        private final Map<Comparable<?>, Integer> toValueIndex;

        protected GenericIndexer(IProperty<T> property) {
            super(property, property.getAllowedValues().toArray((T[]) new Comparable[0]));
            // use a mutable map first to detect repeated values
            Map<Comparable<?>, Integer> tempMap = new LinkedHashMap<>();
            for (int i = 0; i < this.valuesInOrder.length; i++) {
                tempMap.put(this.valuesInOrder[i], i);
                /*
                if(oldI != null) {
                    VintageFix.LOGGER.warn("Property {} uses the same value {} multiple times", property.getClass().getName(), this.valuesInOrder[i]);
                }
                */
            }
            this.toValueIndex = ImmutableMap.copyOf(tempMap);
        }

        @Override
        public int toIndex(T value) {
            return toValueIndex.getOrDefault(value, -1);
        }
    }
}
