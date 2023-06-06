package org.embeddedt.vintagefix.dynamicresources;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.EventBus;
import net.minecraftforge.fml.common.eventhandler.IEventListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.util.function.Consumer;

public class EventUtil {
    private static final Logger LOGGER = LogManager.getLogger();

    public static IEventListener[] getListenersForEvent(Event event) {
        int busID;
        try {
            Field busIDField = EventBus.class.getDeclaredField("busID");
            busIDField.setAccessible(true);
            busID = busIDField.getInt(MinecraftForge.EVENT_BUS);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }

        IEventListener[] listeners = event.getListenerList().getListeners(busID);
        return listeners;
    }
}
