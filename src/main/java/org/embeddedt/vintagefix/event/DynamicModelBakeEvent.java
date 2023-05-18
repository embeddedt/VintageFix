package org.embeddedt.vintagefix.event;

import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.fml.common.eventhandler.Event;

public class DynamicModelBakeEvent extends Event {
    public final ResourceLocation location;
    public final IModel unbakedModel;
    public IBakedModel bakedModel;

    public DynamicModelBakeEvent(ResourceLocation location, IModel unbakedModel, IBakedModel bakedModel) {
        this.location = location;
        this.unbakedModel = unbakedModel;
        this.bakedModel = bakedModel;
    }
}
