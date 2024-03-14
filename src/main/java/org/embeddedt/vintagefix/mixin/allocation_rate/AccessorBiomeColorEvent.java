package org.embeddedt.vintagefix.mixin.allocation_rate;

import net.minecraftforge.event.terraingen.BiomeEvent;
import org.embeddedt.vintagefix.annotation.ClientOnlyMixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BiomeEvent.BiomeColor.class)
@ClientOnlyMixin
public interface AccessorBiomeColorEvent {
    @Accessor(value = "originalColor", remap = false)
    @Mutable
    void setOriginalColor(int value);
}
