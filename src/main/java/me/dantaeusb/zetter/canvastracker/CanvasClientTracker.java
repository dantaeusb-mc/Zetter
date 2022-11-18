package me.dantaeusb.zetter.canvastracker;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.client.renderer.CanvasRenderer;
import me.dantaeusb.zetter.core.ZetterNetwork;
import me.dantaeusb.zetter.event.*;
import me.dantaeusb.zetter.network.packet.CCanvasUnloadRequestPacket;
import me.dantaeusb.zetter.storage.AbstractCanvasData;
import com.google.common.collect.Maps;
import me.dantaeusb.zetter.storage.CanvasData;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;

import javax.annotation.Nullable;
import java.util.Map;

public class CanvasClientTracker implements ICanvasTracker {
    private final Level world;
    Map<String, AbstractCanvasData> canvases = Maps.newHashMap();
    Map<String, Long> timestamps = Maps.newHashMap();

    public CanvasClientTracker(Level world) {
        super();

        this.world = world;
    }

    @Override
    @Nullable
    public <T extends AbstractCanvasData> T getCanvasData(String canvasCode) {
        return (T) this.canvases.get(canvasCode);
    }

    /**
     * Register texture here in canvas tracker on client
     *
     * @param canvasData
     */
    @Override
    public void registerCanvasData(String canvasCode, AbstractCanvasData canvasData, long timestamp) {
        if (this.timestamps.containsKey(canvasCode) && this.timestamps.get(canvasCode) > timestamp) {
            Zetter.LOG.warn("Trying to sync canvas with an older texture!");
            return;
        }

        CanvasRenderPreRegisterEvent preEvent = new CanvasRenderPreRegisterEvent(canvasCode, canvasData, timestamp);
        MinecraftForge.EVENT_BUS.post(preEvent);

        if (!preEvent.isCanceled()) {
            this.canvases.put(canvasCode, canvasData);
            this.timestamps.put(canvasCode, timestamp);

            CanvasRenderer.getInstance().addCanvas(canvasCode, canvasData);
        }

        CanvasRenderPostRegisterEvent postEvent = new CanvasRenderPostRegisterEvent(canvasCode, canvasData, timestamp);
        MinecraftForge.EVENT_BUS.post(postEvent);
    }

    @Override
    public void unregisterCanvasData(String removedCanvasCode) {
        AbstractCanvasData canvasData = this.getCanvasData(removedCanvasCode);

        if (canvasData == null) {
            Zetter.LOG.error("Cannot unregister not-registered canvas " + removedCanvasCode);
            return;
        }

        CanvasRenderPreUnregisterEvent preEvent = new CanvasRenderPreUnregisterEvent(removedCanvasCode);
        MinecraftForge.EVENT_BUS.post(preEvent);

        // Remove existing entry if we have one to replace with a new one
        this.canvases.remove(removedCanvasCode);

        CanvasRenderer.getInstance().removeCanvas(removedCanvasCode);

        if (canvasData.isManaged()) {
            // Notifying server that we're no longer tracking it
            CCanvasUnloadRequestPacket unloadPacket = new CCanvasUnloadRequestPacket(removedCanvasCode);
            ZetterNetwork.simpleChannel.sendToServer(unloadPacket);
        }

        CanvasRenderPostUnregisterEvent postEvent = new CanvasRenderPostUnregisterEvent(removedCanvasCode);
        MinecraftForge.EVENT_BUS.post(postEvent);
    }

    @Override
    public Level getWorld() {
        return this.world;
    }
}
