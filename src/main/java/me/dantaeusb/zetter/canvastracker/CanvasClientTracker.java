package me.dantaeusb.zetter.canvastracker;

import me.dantaeusb.zetter.client.renderer.CanvasRenderer;
import me.dantaeusb.zetter.event.CanvasRegisterEvent;
import me.dantaeusb.zetter.storage.AbstractCanvasData;
import com.google.common.collect.Maps;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;

import javax.annotation.Nullable;
import java.util.Map;

public class CanvasClientTracker extends CanvasDefaultTracker  {
    private final Level world;
    Map<String, AbstractCanvasData> canvases = Maps.newHashMap();

    public CanvasClientTracker(Level world) {
        super();

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

        CanvasRegisterEvent event = new CanvasRegisterEvent(newCanvasCode, newCanvasData);

        MinecraftForge.EVENT_BUS.post(event);
    }

    @Override
    public void unregisterCanvasData(String newCanvasCode) {
        // Remove existing entry if we have one to replace with a new one
        this.canvases.remove(newCanvasCode);

        CanvasRenderer.getInstance().unloadCanvas(newCanvasCode);
    }

    @Override
    public Level getWorld() {
        return this.world;
    }
}
