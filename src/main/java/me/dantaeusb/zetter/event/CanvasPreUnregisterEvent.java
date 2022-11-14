package me.dantaeusb.zetter.event;

import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

@Cancelable
public class CanvasPreUnregisterEvent extends Event {
    public final String canvasCode;

    public CanvasPreUnregisterEvent(String canvasCode) {
        this.canvasCode = canvasCode;
    }
}
