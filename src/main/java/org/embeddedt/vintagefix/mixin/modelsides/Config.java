package org.embeddedt.vintagefix.mixin.modelsides;

import org.embeddedt.vintagefix.config.FerriteConfig;
import org.embeddedt.vintagefix.config.FerriteMixinConfig;

public class Config extends FerriteMixinConfig {
    public Config() {
        super(FerriteConfig.MODEL_SIDES);
    }
}
