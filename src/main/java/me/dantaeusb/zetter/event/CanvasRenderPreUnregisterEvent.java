package me.dantaeusb.zetter.event;

import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

@Cancelable
public class CanvasRenderPreUnregisterEvent extends Event {
    public final String canvasCode;

    public CanvasRenderPreUnregisterEvent(String canvasCode) {
        this.canvasCode = canvasCode;
    }
}
