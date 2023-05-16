package org.embeddedt.vintagefix.mixin.fastmap;

import org.embeddedt.vintagefix.config.FerriteConfig;
import org.embeddedt.vintagefix.config.FerriteMixinConfig;

public class Config extends FerriteMixinConfig {
    public Config() {
        super(FerriteConfig.NEIGHBOR_LOOKUP);
    }
}
