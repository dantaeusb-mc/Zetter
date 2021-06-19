package com.dantaeusb.zetter.canvastracker;

import com.dantaeusb.zetter.Zetter;
import com.dantaeusb.zetter.client.renderer.CanvasRenderer;
import com.dantaeusb.zetter.storage.AbstractCanvasData;
import com.dantaeusb.zetter.storage.CanvasData;
import com.google.common.collect.Maps;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.Map;

public class CanvasClientTracker extends CanvasDefaultTracker  {
    private final World world;
    Map<String, AbstractCanvasData> canvases = Maps.newHashMap();

    public CanvasClientTracker(World world) {
        this.world = world;
        Zetter.LOG.info("CanvasClientTracker");
    }

    @Override
    @Nullable
    public <T extends AbstractCanvasData> T getCanvasData(String canvasCode, @Nullable Class<T> type) {
        return (T) this.canvases.get(canvasCode);
    }

    /**
     * We can't replace the object cause it'll keep references to old object
     * in GUI and renderer.
     *
     * @todo: I'm not sure what's wrong about that statement above; we might actually need
     * @todo: it in order to avoid triggering GUIs to update and not bother GC
     *
     * @param newCanvasData
     */
    @Override
    public void registerCanvasData(AbstractCanvasData newCanvasData) {
        // Remove existing entry if we have one to replace with a new one
        this.canvases.remove(newCanvasData.getName());
        this.canvases.put(newCanvasData.getName(), newCanvasData);

        CanvasRenderer.getInstance().addCanvas(newCanvasData);
    }

    @Override
    public World getWorld() {
        return this.world;
    }
}
