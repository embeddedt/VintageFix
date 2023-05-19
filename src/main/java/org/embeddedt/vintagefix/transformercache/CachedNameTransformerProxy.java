package org.embeddedt.vintagefix.transformercache;

import net.minecraft.launchwrapper.IClassNameTransformer;
import net.minecraft.launchwrapper.IClassTransformer;

public class CachedNameTransformerProxy extends CachedTransformerProxy implements IClassTransformer, IClassNameTransformer {

    public CachedNameTransformerProxy(IClassTransformer original) {
        super(original);
    }

    @Override
    public String unmapClassName(String name) {
        return ((IClassNameTransformer)original).unmapClassName(name);
    }

    @Override
    public String remapClassName(String name) {
        return ((IClassNameTransformer)original).remapClassName(name);
    }

}
