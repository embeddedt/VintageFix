package org.embeddedt.vintagefix.mixin.mrl;

import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ResourceLocation.class)
public interface ResourceLocationAccess {
    @Accessor
    @Mutable
    void setNamespace(String newNamespace);

    @Accessor
    @Mutable
    void setPath(String newPath);
}
