package org.embeddedt.vintagefix.util;

import com.google.common.base.Predicate;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.multipart.ICondition;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PredicateHelper {
    public static List<Predicate<IBlockState>> toCanonicalList(
        Iterable<? extends ICondition> conditions, BlockStateContainer stateContainer
    ) {
        List<Predicate<IBlockState>> list = new ArrayList<>();
        for (ICondition cond : conditions) {
            list.add(cond.getPredicate(stateContainer));
        }
        canonize(list);
        return list;
    }

    /**
     * Sorts the given list by hashcode. This means that passing in different permutations of the same predicates will
     * usually result in the same list (ignoring hash collisions).
     */
    public static <T> void canonize(List<Predicate<T>> input) {
        input.sort(Comparator.comparingInt(Predicate::hashCode));
        if (input instanceof ArrayList) {
            ((ArrayList<Predicate<T>>)input).trimToSize();
        }
    }

    public static <T> Predicate<T> and(List<Predicate<T>> list) {
        return state -> {
            for (Predicate<T> predicate : list) {
                if (!predicate.test(state)) {
                    return false;
                }
            }
            return true;
        };
    }

    public static <T> Predicate<T> or(List<Predicate<T>> list) {
        return state -> {
            for (Predicate<T> predicate : list) {
                if (predicate.test(state)) {
                    return true;
                }
            }
            return false;
        };
    }
}
