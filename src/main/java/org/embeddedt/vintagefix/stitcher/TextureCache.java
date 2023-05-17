package org.embeddedt.vintagefix.stitcher;

import net.minecraft.util.ResourceLocation;

import java.awt.image.BufferedImage;
import java.util.concurrent.ConcurrentHashMap;

public class TextureCache {
    public static final ConcurrentHashMap<ResourceLocation, BufferedImage> textureLoadingCache = new ConcurrentHashMap<>();
}
