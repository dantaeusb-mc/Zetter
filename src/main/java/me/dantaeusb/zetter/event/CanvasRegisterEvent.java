package me.dantaeusb.zetter.event;

import me.dantaeusb.zetter.storage.AbstractCanvasData;
import net.minecraftforge.eventbus.api.Event;

public class CanvasRegisterEvent extends Event {
    private final String canvasCode;
    private final AbstractCanvasData canvasData;

    public CanvasRegisterEvent(String canvasCode, AbstractCanvasData canvasData) {
        this.canvasCode = canvasCode;
        this.canvasData =canvasData;
    }

    public String getCanvasCode() {
        return this.canvasCode;
    }

    public AbstractCanvasData getCanvasData() {
        return this.canvasData;
    }
}
