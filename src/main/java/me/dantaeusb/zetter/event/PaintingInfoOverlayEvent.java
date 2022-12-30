package me.dantaeusb.zetter.event;

import me.dantaeusb.zetter.client.gui.overlay.PaintingInfoOverlay;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

/**
 * Called when painting overlay shown
 * Used to hide previous overlay, even if its an
 * overlay of another type
 */
@Cancelable
public class PaintingInfoOverlayEvent extends Event {
    public final PaintingInfoOverlay overlay;

    public PaintingInfoOverlayEvent(PaintingInfoOverlay overlay) {
        this.overlay = overlay;
    }
}
