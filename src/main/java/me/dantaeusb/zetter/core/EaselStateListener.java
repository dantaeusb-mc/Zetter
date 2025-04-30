package me.dantaeusb.zetter.core;

import me.dantaeusb.zetter.entity.item.state.CanvasState;

public interface EaselStateListener {
    /**
     * Notifies listeners that canvas is being initialized
     * Used by menu to avoid sending stack update before
     * initialization packet
     * @param state
     */
    void stateCanvasInitializationStart(CanvasState state);

    /**
     * Notifies listeners that canvas is being initialized
     * Used by menu to continue sending stack update after
     * initialization packet
     * @param state
     */
    void stateCanvasInitializationEnd(CanvasState state);

    /**
     * Canvas changed
     * @param state
     */
    void stateChanged(CanvasState state);
}
