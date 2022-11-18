package me.dantaeusb.zetter.event;

import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

@Cancelable
public class CanvasServerPreUnregisterEvent extends Event {
    public final String canvasCode;

    public CanvasServerPreUnregisterEvent(String canvasCode) {
        this.canvasCode = canvasCode;
    }
}
