package com.dantaeusb.immersivemp.locks.capability.canvastracker;

import com.dantaeusb.immersivemp.ImmersiveMp;
import com.dantaeusb.immersivemp.locks.world.storage.CanvasData;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class CanvasServerTracker extends CanvasDefaultTracker {
    private final World world;
    private int lastId;

    public CanvasServerTracker(World world) {
        this.world = world;
        this.lastId = 0;
        ImmersiveMp.LOG.info("CanvasServerTracker");
    }

    @Override
    public World getWorld() {
        return this.world;
    }

    @Override
    public int getNextId() {
        return ++this.lastId;
    }
    public int getLastId() {
        return this.lastId;
    }

    public void setLastId(int id) {
        this.lastId = id;
    }

    @Override
    @Nullable
    public CanvasData getCanvasData(String canvasName) {
        return this.world.getServer().func_241755_D_().getSavedData().get(() -> new CanvasData(canvasName), canvasName);
    }

    /**
     * func_241755_D_ = getOverworld
     */
    @Override
    public void registerCanvasData(CanvasData canvasData) {
        this.world.getServer().func_241755_D_().getSavedData().set(canvasData);
    }
}
