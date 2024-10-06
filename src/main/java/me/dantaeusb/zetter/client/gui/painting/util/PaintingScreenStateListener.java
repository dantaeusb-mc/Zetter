package me.dantaeusb.zetter.client.gui.painting.util;

import me.dantaeusb.zetter.client.gui.painting.util.state.PaintingScreenState;

public interface PaintingScreenStateListener {
    void onPaintingScreenStateChange(PaintingScreenState newState);
}
