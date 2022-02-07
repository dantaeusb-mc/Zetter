package me.dantaeusb.zetter.canvastracker;

import me.dantaeusb.zetter.storage.AbstractCanvasData;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

public interface ICanvasTracker {
    String NBT_TAG_LAST_CANVAS_ID = "LastCanvasId";
    String NBT_TAG_LAST_PAINTING_ID = "LastPaintingId";

    int getNextCanvasId();

    int getLastCanvasId();

    void setLastCanvasId(int id);

    int getNextPaintingId();

    int getLastPaintingId();

    void setLastPaintingId(int id);

    Level getWorld();

    <T extends AbstractCanvasData> T getCanvasData(String canvasCode, @Nullable Class<T> type);

    void registerCanvasData(String canvasCode, AbstractCanvasData canvasData);

    void unregisterCanvasData(String canvasCode);

    Tag serializeNBT();

    void deserializeNBT(Tag nbt);
}
