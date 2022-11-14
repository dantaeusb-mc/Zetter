package me.dantaeusb.zetter.canvastracker;

import me.dantaeusb.zetter.storage.AbstractCanvasData;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.BitSet;
import java.util.function.Supplier;

public interface ICanvasTracker {
    Level getWorld();

    <T extends AbstractCanvasData> T getCanvasData(String canvasCode);

    default void registerCanvasData(String canvasCode, AbstractCanvasData canvasData) {
        registerCanvasData(canvasCode, canvasData, System.currentTimeMillis());
    }

    void registerCanvasData(String canvasCode, AbstractCanvasData canvasData, long timestamp);

    void unregisterCanvasData(String canvasCode);
}
