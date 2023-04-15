package me.dantaeusb.zetter.event;

import me.dantaeusb.zetter.storage.AbstractCanvasData;
import me.dantaeusb.zetter.storage.CanvasDataType;
import net.minecraftforge.eventbus.api.Event;

/**
 * Called when painting overlay shown
 * Used to hide previous overlay, even if its an
 * overlay of another type
 */
public class CanvasOverlayViewEvent<T extends AbstractCanvasData> extends Event {
    public final T canvasData;

    public CanvasOverlayViewEvent(T canvasData) {
        this.canvasData = canvasData;
    }
}
