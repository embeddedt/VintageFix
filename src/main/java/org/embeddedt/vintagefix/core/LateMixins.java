package org.embeddedt.vintagefix.core;

import com.google.common.collect.ImmutableList;
import zone.rong.mixinbooter.ILateMixinLoader;

import java.util.List;

public class LateMixins implements ILateMixinLoader {
    @Override
    public List<String> getMixinConfigs() {
        return ImmutableList.of("mixins.vintagefix.late.json");
    }
}
