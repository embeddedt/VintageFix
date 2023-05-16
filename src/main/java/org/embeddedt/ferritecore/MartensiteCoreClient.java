package org.embeddedt.ferritecore;

import malte0811.ferritecore.impl.Deduplicator;
import net.minecraftforge.client.event.ColorHandlerEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class MartensiteCoreClient {
    @SubscribeEvent
    public void registerListener(ColorHandlerEvent.Block event) {
        Deduplicator.registerReloadListener();
    }
}
