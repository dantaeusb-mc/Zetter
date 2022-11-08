package me.dantaeusb.zetter.event;

import me.dantaeusb.zetter.storage.AbstractCanvasData;
import net.minecraftforge.eventbus.api.Event;

public class CanvasRegisterEvent extends Event {
    public final String canvasCode;
    public final AbstractCanvasData canvasData;

    public CanvasRegisterEvent(String canvasCode, AbstractCanvasData canvasData) {
        this.canvasCode = canvasCode;
        this.canvasData = canvasData;
    }
}
