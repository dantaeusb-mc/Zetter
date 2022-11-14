package me.dantaeusb.zetter.event;

import net.minecraftforge.eventbus.api.Event;

public class CanvasPostUnregisterEvent extends Event {
    public final String canvasCode;

    public CanvasPostUnregisterEvent(String canvasCode) {
        this.canvasCode = canvasCode;
    }
}
