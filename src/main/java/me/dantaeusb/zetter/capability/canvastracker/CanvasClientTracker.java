package me.dantaeusb.zetter.capability.canvastracker;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.client.renderer.CanvasRenderer;
import me.dantaeusb.zetter.core.ZetterNetwork;
import me.dantaeusb.zetter.event.*;
import me.dantaeusb.zetter.network.packet.CCanvasUnloadRequestPacket;
import me.dantaeusb.zetter.storage.AbstractCanvasData;
import com.google.common.collect.Maps;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;

import javax.annotation.Nullable;
import java.util.BitSet;
import java.util.Map;

public class CanvasClientTracker implements CanvasTracker {
    private Level level;
    Map<String, AbstractCanvasData> canvases = Maps.newHashMap();
    Map<String, Long> timestamps = Maps.newHashMap();

    public CanvasClientTracker() {
        super();
    }

    @Override
    public void setLevel(Level level) {
        if (this.level != null) {
            throw new IllegalStateException("Cannot change level for capability");
        }

        this.level = level;
    }

    @Override
    public Level getLevel() {
        return this.level;
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

        CanvasRegisterEvent.Pre preEvent = new CanvasRegisterEvent.Pre(canvasCode, canvasData, this.level, timestamp);
        MinecraftForge.EVENT_BUS.post(preEvent);

        if (!preEvent.isCanceled()) {
            this.canvases.put(canvasCode, canvasData);
            this.timestamps.put(canvasCode, timestamp);

            CanvasRenderer.getInstance().addCanvas(canvasCode, canvasData);
        }

        CanvasRegisterEvent.Post postEvent = new CanvasRegisterEvent.Post(canvasCode, canvasData, this.level, timestamp);
        MinecraftForge.EVENT_BUS.post(postEvent);
    }

    @Override
    public void unregisterCanvasData(String removedCanvasCode) {
        AbstractCanvasData canvasData = this.getCanvasData(removedCanvasCode);

        if (canvasData == null) {
            Zetter.LOG.error("Cannot unregister not-registered canvas " + removedCanvasCode);
            return;
        }

        long timestamp = System.currentTimeMillis();

        CanvasUnregisterEvent.Pre preEvent = new CanvasUnregisterEvent.Pre(removedCanvasCode, canvasData, this.level, timestamp);
        MinecraftForge.EVENT_BUS.post(preEvent);

        // Remove existing entry if we have one to replace with a new one
        this.canvases.remove(removedCanvasCode);

        CanvasRenderer.getInstance().removeCanvas(removedCanvasCode);

        if (canvasData.isManaged()) {
            // Notifying server that we're no longer tracking it
            CCanvasUnloadRequestPacket unloadPacket = new CCanvasUnloadRequestPacket(removedCanvasCode);
            ZetterNetwork.simpleChannel.sendToServer(unloadPacket);
        }

        CanvasUnregisterEvent.Post postEvent = new CanvasUnregisterEvent.Post(removedCanvasCode, canvasData, this.level, timestamp);
        MinecraftForge.EVENT_BUS.post(postEvent);
    }

    /*
     * This is only for server
     */

    @Override
    public BitSet getCanvasIds() {
        throw new IllegalStateException("Client does not store authoritative canvas tracking information");
    }

    @Override
    public void setCanvasIds(BitSet canvasIds) {
        throw new IllegalStateException("Client does not store authoritative canvas tracking information");
    }

    @Override
    public int getLastCanvasId() {
        throw new IllegalStateException("Client does not store authoritative canvas tracking information");
    }

    @Override
    public void setLastPaintingId(int id) {
        throw new IllegalStateException("Client does not store authoritative canvas tracking information");
    }

    @Override
    public int getLastPaintingId() {
        throw new IllegalStateException("Client does not store authoritative canvas tracking information");
    }
}
