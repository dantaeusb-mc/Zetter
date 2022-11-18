package me.dantaeusb.zetter.event;

import net.minecraftforge.eventbus.api.Event;

public class CanvasServerPostUnregisterEvent extends Event {
    public final String canvasCode;

    public CanvasServerPostUnregisterEvent(String canvasCode) {
        this.canvasCode = canvasCode;
    }
}
