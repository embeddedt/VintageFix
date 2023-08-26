package org.embeddedt.vintagefix.ducks;

import net.minecraft.util.ResourceLocation;

import java.util.Set;

public interface IDroppingStitcher {
    void dropLargestSprite();
    void retainAllSprites(Set<ResourceLocation> spriteLocations);
}
