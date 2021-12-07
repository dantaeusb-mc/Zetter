package com.dantaeusb.zetter.canvastracker;

import com.dantaeusb.zetter.Zetter;
import com.dantaeusb.zetter.client.renderer.CanvasRenderer;
import com.dantaeusb.zetter.storage.AbstractCanvasData;
import com.dantaeusb.zetter.storage.CanvasData;
import com.google.common.collect.Maps;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.Map;

public class CanvasClientTracker extends CanvasDefaultTracker  {
    private final Level world;
    Map<String, AbstractCanvasData> canvases = Maps.newHashMap();

    public CanvasClientTracker(Level world) {
        this.world = world;
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
    public void registerCanvasData(String newCanvasCode, AbstractCanvasData newCanvasData) {
        // Remove existing entry if we have one to replace with a new one
        this.canvases.remove(newCanvasCode);
        this.canvases.put(newCanvasCode, newCanvasData);

        CanvasRenderer.getInstance().addCanvas(newCanvasCode, newCanvasData);
    }

    @Override
    public Level getWorld() {
        return this.world;
    }
}
