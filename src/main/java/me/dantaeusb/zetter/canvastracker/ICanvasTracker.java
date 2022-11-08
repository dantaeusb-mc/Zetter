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

    void registerCanvasData(String canvasCode, AbstractCanvasData canvasData);

    void unregisterCanvasData(String canvasCode);
}
