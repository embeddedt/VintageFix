package org.embeddedt.vintagefix.mixin.resourcepacks;

import net.minecraft.client.resources.AbstractResourcePack;
import net.minecraft.util.ResourceLocation;
import org.embeddedt.vintagefix.annotation.ClientOnlyMixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(AbstractResourcePack.class)
@ClientOnlyMixin
public class MixinAbstractResourcePack {
    /**
     * @author embeddedt
     * @reason reduce method size & possibly allocation rate
     */
    @Overwrite
    private static String locationToName(ResourceLocation location) {
        String namespace = location.getNamespace();
        String path = location.getPath();
        // uses a StringBuilder internally
        return "assets/" +
            namespace +
            '/' +
            path;
    }
}
