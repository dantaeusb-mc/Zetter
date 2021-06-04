package com.dantaeusb.zetter.canvastracker;

import com.dantaeusb.zetter.storage.AbstractCanvasData;
import com.dantaeusb.zetter.storage.CanvasData;
import net.minecraft.world.World;

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

    World getWorld();

    <T extends AbstractCanvasData> T getCanvasData(String canvasCode, @Nullable Class<T> type);

    void registerCanvasData(AbstractCanvasData canvasData);
}
