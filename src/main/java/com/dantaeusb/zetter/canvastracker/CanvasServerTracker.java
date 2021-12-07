package com.dantaeusb.zetter.canvastracker;

import com.dantaeusb.zetter.Zetter;
import com.dantaeusb.zetter.core.ModNetwork;
import com.dantaeusb.zetter.network.packet.SCanvasSyncMessage;
import com.dantaeusb.zetter.storage.AbstractCanvasData;
import com.dantaeusb.zetter.storage.CanvasData;
import com.dantaeusb.zetter.storage.DummyCanvasData;
import com.dantaeusb.zetter.storage.PaintingData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraftforge.fmllegacy.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Vector;

public class CanvasServerTracker extends CanvasDefaultTracker {
    private final Level world;
    private int lastCanvasId;
    private int lastPaintingId;

    private final Map<String, Vector<PlayerTrackingCanvas>> trackedCanvases = new HashMap<>();
    private final Vector<String> desyncCanvases = new Vector<>();
    private int ticksFromLastSync = 0;

    public CanvasServerTracker(Level world) {
        this.world = world;
        this.lastCanvasId = 0;
        this.lastPaintingId = 0;
    }

    @Override
    public Level getWorld() {
        return this.world;
    }

    @Override
    public int getNextCanvasId() {
        return ++this.lastCanvasId;
    }

    public int getLastCanvasId() {
        return this.lastCanvasId;
    }

    public void setLastCanvasId(int id) {
        this.lastCanvasId = id;
    }

    @Override
    public int getNextPaintingId() {
        return ++this.lastPaintingId;
    }

    public int getLastPaintingId() {
        return this.lastPaintingId;
    }

    public void setLastPaintingId(int id) {
        this.lastPaintingId = id;
    }

    public void markCanvasDesync(String canvasCode) {
        this.desyncCanvases.add(canvasCode);
    }

    @Override
    @Nullable
    public <T extends AbstractCanvasData> T getCanvasData(String canvasCode, @Nullable Class<T> type) {
        return (T) this.world.getServer().overworld().getDataStorage().get(
            (compoundTag) -> {
                if (type.equals(CanvasData.class)) {
                    return CanvasData.createLoaded(compoundTag);
                } else if (type.equals(PaintingData.class)) {
                    return PaintingData.createLoaded(compoundTag);
                }

                return DummyCanvasData.createDummy();
            },
            canvasCode
        );
    }

    /**
     * Just replacing the object that is serialized - maybe may need to be sure
     * that the object is GC'd
     * We can't do that on client cause renderers are using object reference,
     * and changing object would disconnect it from reference
     * overworld = getOverworld
     */
    @Override
    public void registerCanvasData(String canvasCode, AbstractCanvasData canvasData) {
        if (canvasData instanceof DummyCanvasData) {
            Zetter.LOG.error("Trying to register dummy canvas on server side");
            return;
        }

        this.world.getServer().overworld().getDataStorage().set(canvasCode, canvasData);
    }

    /**
     * Server handling - ticking, tracking from players and syncing
     */

    public void tick() {
        this.ticksFromLastSync++;

        if (this.ticksFromLastSync < 20) {
            return;
        }

        /**
         * Send canvas sync message to every tracking player entity
         */
        MinecraftServer server = this.world.getServer();

        for (String canvasCode : this.desyncCanvases) {
            for (PlayerTrackingCanvas playerTrackingCanvas : this.getTrackingEntries(canvasCode)) {
                ServerPlayer playerEntity = server.getPlayerList().getPlayer(playerTrackingCanvas.playerId);

                SCanvasSyncMessage canvasSyncMessage = new SCanvasSyncMessage(canvasCode, this.getCanvasData(canvasCode, DummyCanvasData.class), System.currentTimeMillis());
                ModNetwork.simpleChannel.send(PacketDistributor.PLAYER.with(() -> playerEntity), canvasSyncMessage);
            }
        }

        this.desyncCanvases.clear();
        this.ticksFromLastSync = 0;
    }

    /**
     * @todo check if already tracking
     * @param playerId
     * @param canvasName
     */
    public void trackCanvas(UUID playerId, String canvasName) {
        Vector<PlayerTrackingCanvas> trackingEntries = this.getTrackingEntries(canvasName);

        for (PlayerTrackingCanvas playerTrackingCanvas : trackingEntries) {
            if (playerTrackingCanvas.playerId == playerId) {
                return;
            }
        }

        trackingEntries.add(new PlayerTrackingCanvas(playerId, canvasName));
    }

    public void stopTrackingCanvas(UUID playerId, String canvasName) {
        Vector<PlayerTrackingCanvas> trackingEntries = this.getTrackingEntries(canvasName);
        trackingEntries.removeIf((PlayerTrackingCanvas entry) -> entry.playerId == playerId);
    }

    public void stopTrackingAllCanvases(UUID playerId) {
        for (String canvasName : this.trackedCanvases.keySet()) {
            this.stopTrackingCanvas(playerId, canvasName);
        }
    }

    private Vector<PlayerTrackingCanvas> getTrackingEntries(String canvasName) {
        return this.trackedCanvases.computeIfAbsent(canvasName, k -> new Vector<>());
    }

    private static class PlayerTrackingCanvas {
        public final UUID playerId;
        public final String canvasName;

        PlayerTrackingCanvas(UUID playerId, String canvasName) {
            this.playerId = playerId;
            this.canvasName = canvasName;
        }
    }
}
