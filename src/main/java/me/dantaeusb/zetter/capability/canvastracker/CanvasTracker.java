package me.dantaeusb.zetter.capability.canvastracker;

import me.dantaeusb.zetter.storage.AbstractCanvasData;
import net.minecraft.world.level.Level;

public interface CanvasTracker {
    void setLevel(Level level);

    Level getLevel();

    <T extends AbstractCanvasData> T getCanvasData(String canvasCode);

    default void registerCanvasData(String canvasCode, AbstractCanvasData canvasData) {
        registerCanvasData(canvasCode, canvasData, System.currentTimeMillis());
    }

    void registerCanvasData(String canvasCode, AbstractCanvasData canvasData, long timestamp);

    void unregisterCanvasData(String canvasCode);
}
