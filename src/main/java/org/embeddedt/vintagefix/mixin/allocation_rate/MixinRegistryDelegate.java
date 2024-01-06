package org.embeddedt.vintagefix.mixin.allocation_rate;

import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(targets = { "net/minecraftforge/registries/RegistryDelegate" }, remap = false)
public class MixinRegistryDelegate {
    @Shadow
    private ResourceLocation name;

    /**
     * @author embeddedt
     * @reason avoid allocation spam from calling Objects.hash
     */
    @Overwrite
    public int hashCode()
    {
        return this.name == null ? 0 : this.name.hashCode();
    }
}
