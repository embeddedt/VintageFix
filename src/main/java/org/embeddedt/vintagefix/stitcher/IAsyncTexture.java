package org.embeddedt.vintagefix.stitcher;

import net.minecraft.client.resources.IResourceManager;

import java.util.concurrent.Executor;

public interface IAsyncTexture {
    void runAsyncLoadPortion(IResourceManager manager, Executor executor);
}
