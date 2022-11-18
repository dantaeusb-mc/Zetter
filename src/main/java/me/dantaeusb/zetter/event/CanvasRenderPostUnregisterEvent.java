package me.dantaeusb.zetter.event;

import net.minecraftforge.eventbus.api.Event;

public class CanvasRenderPostUnregisterEvent extends Event {
    public final String canvasCode;

    public CanvasRenderPostUnregisterEvent(String canvasCode) {
        this.canvasCode = canvasCode;
    }
}
