package me.dantaeusb.zetter.event;

import me.dantaeusb.zetter.storage.AbstractCanvasData;
import net.minecraftforge.eventbus.api.Event;

/**
 * After new canvas is registered, we might need to do some stuff
 * (like update loading textures in GUIs). For that reason we are
 * triggering this event, with the timestamp when the canvas
 * is updated over the network
 */
public class CanvasServerPostRegisterEvent extends Event {
    public final String canvasCode;
    public final AbstractCanvasData canvasData;
    public final long timestamp;

    public CanvasServerPostRegisterEvent(String canvasCode, AbstractCanvasData canvasData, long timestamp) {
        this.canvasCode = canvasCode;
        this.canvasData = canvasData;
        this.timestamp = timestamp;
    }
}
