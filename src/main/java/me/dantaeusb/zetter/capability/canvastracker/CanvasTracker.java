package me.dantaeusb.zetter.capability.canvastracker;

import me.dantaeusb.zetter.storage.AbstractCanvasData;
import net.minecraft.world.World;

import java.util.BitSet;

public interface CanvasTracker {
    void setLevel(World level);

    World getLevel();

    <T extends AbstractCanvasData> T getCanvasData(String canvasCode);

    default void registerCanvasData(String canvasCode, AbstractCanvasData canvasData) {
        registerCanvasData(canvasCode, canvasData, System.currentTimeMillis());
    }

    void registerCanvasData(String canvasCode, AbstractCanvasData canvasData, long timestamp);
    void unregisterCanvasData(String canvasCode);

    BitSet getCanvasIds();
    void setCanvasIds(BitSet canvasIds);

    int getLastCanvasId();

    void setLastPaintingId(int id);
    int getLastPaintingId();
}
