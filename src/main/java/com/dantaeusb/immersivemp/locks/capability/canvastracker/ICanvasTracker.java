package com.dantaeusb.immersivemp.locks.capability.canvastracker;

import com.dantaeusb.immersivemp.locks.world.storage.CanvasData;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
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
