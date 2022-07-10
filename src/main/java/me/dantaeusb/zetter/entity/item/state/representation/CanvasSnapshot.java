package me.dantaeusb.zetter.entity.item.state.representation;

import java.util.UUID;

/**
 * Snapshots are complete copy of current canvas
 * at a particular time, they're created only on
 * server side but then send to client for easier
 * state restoration
 */
public class CanvasSnapshot {
    public final UUID id;
    public final Long timestamp;
    public final byte[] colors;
    public final boolean weak;

    private CanvasSnapshot(UUID uuid, byte[] colors, Long snapshotTime, boolean weak) {
        this.id = uuid;
        this.timestamp = snapshotTime;
        this.colors = colors;
        this.weak = weak;
    }

    /**
     * Client-only "weak" snapshot is created when server
     * sends regular canvas sync message (not a snapshot)
     */
    public static CanvasSnapshot createWeakSnapshot(byte[] colors, Long snapshotTime) {
        return new CanvasSnapshot(UUID.randomUUID(), colors, snapshotTime, true);
    }

    /**
     * Client-only regular snapshot sent over the network
     * @param uuid
     * @param colors
     * @param snapshotTime
     * @return
     */
    public static CanvasSnapshot createNetworkSnapshot(UUID uuid, byte[] colors, Long snapshotTime) {
        return new CanvasSnapshot(uuid, colors, snapshotTime, false);
    }

    public static CanvasSnapshot createServerSnapshot(byte[] colors) {
        return new CanvasSnapshot(UUID.randomUUID(), colors, System.currentTimeMillis(), false);
    }
}
