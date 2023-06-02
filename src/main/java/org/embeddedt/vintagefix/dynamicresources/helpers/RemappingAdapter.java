package org.embeddedt.vintagefix.dynamicresources.helpers;

import net.minecraft.client.resources.IResourcePack;
import org.embeddedt.vintagefix.dynamicresources.ResourcePackHelper;

import java.io.IOException;
import java.util.Iterator;
import java.util.function.Function;
import java.util.function.Predicate;

public class RemappingAdapter<T extends IResourcePack> implements ResourcePackHelper.Adapter<T> {
    private final Function<T, IResourcePack> adapterFn;

    public RemappingAdapter(Function<T, IResourcePack> fn) {
        this.adapterFn = fn;
    }

    @Override
    public Iterator<String> getAllPaths(T pack, Predicate<String> filter) throws IOException {
        IResourcePack newPack = adapterFn.apply(pack);
        return ResourcePackHelper.getAllPaths(newPack, filter).iterator();
    }
}
