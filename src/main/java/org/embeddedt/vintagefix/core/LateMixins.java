package org.embeddedt.vintagefix.core;

import com.google.common.collect.ImmutableList;
import zone.rong.mixinbooter.ILateMixinLoader;

import java.util.List;

public class LateMixins implements ILateMixinLoader {
    static boolean atLateStage = false;
    @Override
    public List<String> getMixinConfigs() {
        atLateStage = true;
        return ImmutableList.of("mixins.vintagefix.late.json");
    }
}
