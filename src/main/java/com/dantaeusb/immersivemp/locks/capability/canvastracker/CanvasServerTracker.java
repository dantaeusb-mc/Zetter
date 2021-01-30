package com.dantaeusb.immersivemp.locks.capability.canvastracker;

import com.dantaeusb.immersivemp.ImmersiveMp;
import com.dantaeusb.immersivemp.locks.core.ModLockNetwork;
import com.dantaeusb.immersivemp.locks.network.packet.painting.SCanvasSyncMessage;
import com.dantaeusb.immersivemp.locks.world.storage.CanvasData;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Vector;

public class CanvasServerTracker extends CanvasDefaultTracker {
    private final World world;
    private int lastId;

    private final Map<String, Vector<PlayerTrackingCanvas>> trackedCanvases = new HashMap<>();
    private final Vector<String> desyncCanvases = new Vector<>();
    private int ticksFromLastSync = 0;

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

    public void markCanvasDesync(String canvasName) {
        this.desyncCanvases.add(canvasName);
    }

    @Override
    @Nullable
    public CanvasData getCanvasData(String canvasName) {
        return this.world.getServer().func_241755_D_().getSavedData().get(() -> new CanvasData(canvasName), canvasName);
    }

    /**
     * Just replacing the object that is serialized - maybe may need to be sure
     * that the object is GC'd
     * We can't do that on client cause renderers are using object reference,
     * and changing object would disconnect it from reference
     * func_241755_D_ = getOverworld
     */
    @Override
    public void registerCanvasData(CanvasData canvasData) {
        this.world.getServer().func_241755_D_().getSavedData().set(canvasData);
    }

    /**
     * Server handling - ticking, tracking from players and syncing
     */

    public void tick() {
        this.ticksFromLastSync++;

        // A bit random to avoid thousands of things being made every second
        if (this.ticksFromLastSync < 23) {
            return;
        }

        /**
         * Send canvas sync message to every tracking player entity
         */
        MinecraftServer server = this.world.getServer();

        for (String canvasName : this.desyncCanvases) {
            for (PlayerTrackingCanvas playerTrackingCanvas : this.getTrackingEntries(canvasName)) {
                ServerPlayerEntity playerEntity = server.getPlayerList().getPlayerByUUID(playerTrackingCanvas.playerId);

                SCanvasSyncMessage canvasSyncMessage = new SCanvasSyncMessage(this.getCanvasData(canvasName), System.currentTimeMillis());
                ModLockNetwork.simpleChannel.send(PacketDistributor.PLAYER.with(() -> playerEntity), canvasSyncMessage);
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
        Vector<PlayerTrackingCanvas> trackingEntries = this.trackedCanvases.computeIfAbsent(canvasName, k -> new Vector<>());

        return trackingEntries;
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
