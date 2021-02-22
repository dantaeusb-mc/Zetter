package com.dantaeusb.zetter.canvastracker;

import com.dantaeusb.zetter.Zetter;
import com.dantaeusb.zetter.client.gui.CanvasRenderer;
import com.dantaeusb.zetter.storage.CanvasData;
import com.google.common.collect.Maps;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.Map;

public class CanvasClientTracker extends CanvasDefaultTracker  {
    private final World world;
    Map<String, CanvasData> canvases = Maps.newHashMap();

    private final Map<String, Boolean> requestedCanvases = Maps.newHashMap();

    public CanvasClientTracker(World world) {
        this.world = world;
        Zetter.LOG.info("CanvasClientTracker");
    }

    @Nullable
    public CanvasData getCanvasData(String canvasName) {
        return this.canvases.get(canvasName);
    }

    /**
     * We can't replace the object cause it'll keep references to old object
     * in GUI and renderer.
     * @param newCanvasData
     */
    @Override
    public void registerCanvasData(CanvasData newCanvasData) {
        if (this.canvases.containsKey(newCanvasData.getName())) {
            CanvasData registeredCanvasData = this.canvases.get(newCanvasData.getName());
            registeredCanvasData.copyFrom(newCanvasData);
        } else {
            this.canvases.put(newCanvasData.getName(), newCanvasData);
        }

        CanvasRenderer.getInstance().updateCanvas(newCanvasData);
    }

    @Override
    public World getWorld() {
        return this.world;
    }
}
