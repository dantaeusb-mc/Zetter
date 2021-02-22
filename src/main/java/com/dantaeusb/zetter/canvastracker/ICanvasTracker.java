package com.dantaeusb.zetter.canvastracker;

import com.dantaeusb.zetter.storage.CanvasData;
import net.minecraft.world.World;

public interface ICanvasTracker {
    String NBT_TAG_LAST_ID = "lastId";

    int getNextId();

    int getLastId();

    void setLastId(int id);

    World getWorld();

    CanvasData getCanvasData(String canvasName);

    void registerCanvasData(CanvasData canvasData);
}
