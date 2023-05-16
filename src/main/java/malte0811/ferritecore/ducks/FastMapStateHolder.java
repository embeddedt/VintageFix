package malte0811.ferritecore.ducks;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableTable;
import malte0811.ferritecore.fastmap.FastMap;
import net.minecraft.block.properties.IProperty;

public interface FastMapStateHolder<S> {
    FastMap<S> getStateMap();

    void setStateMap(FastMap<S> newValue);

    int getStateIndex();

    void setStateIndex(int newValue);

    ImmutableMap<IProperty<?>, Comparable<?>> getVanillaPropertyMap();

    void replacePropertyMap(ImmutableMap<IProperty<?>, Comparable<?>> newMap);

    void setNeighborTable(ImmutableTable<IProperty<?>, Comparable<?>, S> table);

    ImmutableTable<IProperty<?>, Comparable<?>, S> getNeighborTable();
}
