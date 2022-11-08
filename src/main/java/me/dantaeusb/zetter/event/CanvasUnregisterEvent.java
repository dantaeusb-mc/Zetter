package me.dantaeusb.zetter.event;

import me.dantaeusb.zetter.storage.AbstractCanvasData;
import net.minecraftforge.eventbus.api.Event;

public class CanvasUnregisterEvent extends Event {
    public final String canvasCode;

    public CanvasUnregisterEvent(String canvasCode) {
        this.canvasCode = canvasCode;
    }
}
