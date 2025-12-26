package org.embeddedt.vintagefix.mixin.dynamic_resources;

import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.eventhandler.IContextSetter;
import org.embeddedt.vintagefix.ducks.IModAwareModelBakeEvent;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Used to attach IContextSetter to ModelBakeEvent, so that we can see which mod is calling getKeys on our wrapped
 * registry.
 */
@Mixin(ModelBakeEvent.class)
public class MixinModelBakeEvent implements IContextSetter, IModAwareModelBakeEvent {
    private ModContainer lastMod;

    @Override
    public void setModContainer(ModContainer mod) {
        this.lastMod = mod;
    }

    @Override
    public ModContainer vfix$getLastMod() {
        return this.lastMod;
    }
}
