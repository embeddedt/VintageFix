package org.embeddedt.vintagefix.mixin.dynamic_resources;

import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.fml.common.eventhandler.IContextSetter;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Used to attach IContextSetter to ModelBakeEvent, so that we can see which mod is calling getKeys on our wrapped
 * registry.
 */
@Mixin(ModelBakeEvent.class)
public class MixinModelBakeEvent implements IContextSetter {
}
