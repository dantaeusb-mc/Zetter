package me.dantaeusb.zetter.event;

import me.dantaeusb.zetter.storage.AbstractCanvasData;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

/**
 * Before new canvas is registered, we might need to do some stuff
 * (like update loading textures in GUIs), for that reason we are
 * triggering this event, with the timestamp when the canvas
 * is updated over the network
 *
 * We need pre-event for some cases like when drawing on an Easel
 * so that we don't have back-and-forth texture update that can
 * cause canvas flashing when editing
 *
 * In that case, event is CANCELABLE, but MAKE SURE YOU
 * UPDATE THE TEXTURE IN APPROPRIATE WAY IF YOU CANCEL THE EVENT!
 */
@Cancelable
public class CanvasRenderPreRegisterEvent extends Event {
    public final String canvasCode;
    public final AbstractCanvasData canvasData;
    public final long timestamp;

    public CanvasRenderPreRegisterEvent(String canvasCode, AbstractCanvasData canvasData, long timestamp) {
        this.canvasCode = canvasCode;
        this.canvasData = canvasData;
        this.timestamp = timestamp;
    }
}
