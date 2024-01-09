package org.embeddedt.vintagefix.mixin.allocation_rate;

import net.minecraft.world.biome.Biome;
import net.minecraftforge.event.terraingen.BiomeEvent;
import org.embeddedt.vintagefix.annotation.ClientOnlyMixin;
import org.embeddedt.vintagefix.util.EventUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Biome.class)
@ClientOnlyMixin
public class MixinBiome {
    private final ThreadLocal<BiomeEvent.GetWaterColor> waterColorEventLocal = ThreadLocal.withInitial(() -> new BiomeEvent.GetWaterColor((Biome)(Object)this, 0));;
    private final ThreadLocal<BiomeEvent.GetGrassColor> grassColorEventLocal = ThreadLocal.withInitial(() -> new BiomeEvent.GetGrassColor((Biome)(Object)this, 0));;
    private final ThreadLocal<BiomeEvent.GetFoliageColor> foliageColorEventLocal = ThreadLocal.withInitial(() -> new BiomeEvent.GetFoliageColor((Biome)(Object)this, 0));

    @Unique
    private void prepareEvent(BiomeEvent.BiomeColor event, int defaultColor) {
        event.setNewColor(defaultColor);
        EventUtils.clearPhase(event);
        ((AccessorBiomeColorEvent)event).setOriginalColor(defaultColor);
    }

    @Redirect(method = "getWaterColorMultiplier", at = @At(value = "NEW", target = "(Lnet/minecraft/world/biome/Biome;I)Lnet/minecraftforge/event/terraingen/BiomeEvent$GetWaterColor;"), remap = false)
    private BiomeEvent.GetWaterColor memoizeWaterObject(Biome biome, int original) {
        BiomeEvent.GetWaterColor event = waterColorEventLocal.get();
        prepareEvent(event, original);
        return event;
    }

    @Redirect(method = "getModdedBiomeGrassColor", at = @At(value = "NEW", target = "(Lnet/minecraft/world/biome/Biome;I)Lnet/minecraftforge/event/terraingen/BiomeEvent$GetGrassColor;"), remap = false)
    private BiomeEvent.GetGrassColor memoizeGrassObject(Biome biome, int original) {
        BiomeEvent.GetGrassColor event = grassColorEventLocal.get();
        prepareEvent(event, original);
        return event;
    }

    @Redirect(method = "getModdedBiomeFoliageColor", at = @At(value = "NEW", target = "(Lnet/minecraft/world/biome/Biome;I)Lnet/minecraftforge/event/terraingen/BiomeEvent$GetFoliageColor;"), remap = false)
    private BiomeEvent.GetFoliageColor memoizeFoliageObject(Biome biome, int original) {
        BiomeEvent.GetFoliageColor event = foliageColorEventLocal.get();
        prepareEvent(event, original);
        return event;
    }
}
