package org.embeddedt.vintagefix.impl;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import org.embeddedt.vintagefix.util.PredicateHelper;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class KeyValueConditionImpl {
    private static final Map<Pair<IProperty<?>, Comparable<?>>, Predicate<IBlockState>> STATE_HAS_PROPERTY_CACHE = new ConcurrentHashMap<>();

    /**
     * A copy of {@link net.minecraft.client.renderer.block.model.multipart.KeyValueCondition#getPredicate(StateDefinition)}
     * since targeting the correct line is near impossible
     */
    public static Predicate<IBlockState> getPredicate(
        BlockStateContainer stateContainer, String key, String value, Splitter splitter
    ) {
        IProperty<?> property = stateContainer.getProperty(key);
        if (property == null) {
            throw new RuntimeException(String.format(
                    "Unknown property '%s' on '%s'", key, stateContainer.getBlock().toString()
            ));
        } else {
            String valueNoInvert = value;
            boolean invert = !valueNoInvert.isEmpty() && valueNoInvert.charAt(0) == '!';
            if (invert) {
                valueNoInvert = valueNoInvert.substring(1);
            }

            List<String> matchedStates = splitter.splitToList(valueNoInvert);
            if (matchedStates.isEmpty()) {
                throw new RuntimeException(String.format(
                        "Empty value '%s' for property '%s' on '%s'",
                        value, key, stateContainer.getBlock().toString()
                ));
            } else {
                Predicate<IBlockState> isMatchedState;
                if (matchedStates.size() == 1) {
                    isMatchedState = getBlockStatePredicate(stateContainer, property, valueNoInvert, key, value);
                } else {
                    List<Predicate<IBlockState>> subPredicates = matchedStates.stream()
                            .map(subValue -> getBlockStatePredicate(stateContainer, property, subValue, key, value))
                            .collect(Collectors.toCollection(ArrayList::new));
                    // This line is the only functional change, but targeting it with anything but Overwrite appears to
                    // be impossible
                    PredicateHelper.canonize(subPredicates);
                    isMatchedState = Deduplicator.or(subPredicates);
                }

                return invert ? (s -> !isMatchedState.test(s)) : isMatchedState;
            }
        }
    }

    private static <T extends Comparable<T>>
    Predicate<IBlockState> getBlockStatePredicate(
            BlockStateContainer container,
            IProperty<T> property,
            String subValue,
            String key,
            String value
    ) {
        Optional<T> optional = property.parseValue(subValue);
        if (!optional.isPresent()) {
            throw new RuntimeException(String.format(
                    "Unknown value '%s' for property '%s' on '%s' in '%s'",
                    subValue, key, container.getBlock().toString(), value
            ));
        } else {
            T unwrapped = optional.get();
            return STATE_HAS_PROPERTY_CACHE.computeIfAbsent(
                    Pair.of(property, unwrapped),
                    pair -> {
                        Comparable<?> valueInt = pair.getRight();
                        IProperty<?> propInt = pair.getLeft();
                        return state -> state.getValue(propInt).equals(valueInt);
                    }
            );
        }
    }
}
