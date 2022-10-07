package me.dantaeusb.zetter.canvastracker;

import me.dantaeusb.zetter.storage.AbstractCanvasData;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.BitSet;

public interface ICanvasTracker {
    Level getWorld();

    <T extends AbstractCanvasData> T getCanvasData(String canvasCode, @Nullable Class<T> type);

    void registerCanvasData(String canvasCode, AbstractCanvasData canvasData);

    void unregisterCanvasData(String canvasCode);
}
