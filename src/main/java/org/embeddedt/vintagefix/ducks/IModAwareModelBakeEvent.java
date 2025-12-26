package org.embeddedt.vintagefix.ducks;

import net.minecraftforge.fml.common.ModContainer;

public interface IModAwareModelBakeEvent {
    ModContainer vfix$getLastMod();
}
