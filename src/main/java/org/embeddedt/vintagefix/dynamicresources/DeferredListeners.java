package org.embeddedt.vintagefix.dynamicresources;

import net.minecraft.client.resources.IResourceManagerReloadListener;

import java.util.ArrayList;
import java.util.List;

public class DeferredListeners {
    public static List<IResourceManagerReloadListener> deferredListeners = new ArrayList<>();
}
