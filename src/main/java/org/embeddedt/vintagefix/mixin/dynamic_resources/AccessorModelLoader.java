package org.embeddedt.vintagefix.mixin.dynamic_resources;

import net.minecraft.client.renderer.block.model.ModelBlockDefinition;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ModelLoader;
import org.embeddedt.vintagefix.annotation.ClientOnlyMixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ModelLoader.class)
@ClientOnlyMixin
public interface AccessorModelLoader {
    @Invoker
    ModelBlockDefinition invokeGetModelBlockDefinition(ResourceLocation location);
}
