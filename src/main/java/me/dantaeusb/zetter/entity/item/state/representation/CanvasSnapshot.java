package me.dantaeusb.zetter.entity.item.state.representation;

import java.util.Random;

/**
 * Snapshots are complete copy of current canvas
 * at a particular time, they're created only on
 * server side but then send to client for easier
 * state restoration
 */
public class CanvasSnapshot {
    private static final Random RANDOM = new Random();

    public final int id;
    public final Long timestamp;
    public final byte[] colors;
    public final boolean weak;

    private CanvasSnapshot(int id, byte[] colors, Long snapshotTime, boolean weak) {
        this.id = id;
        this.timestamp = snapshotTime;
        this.colors = colors;
        this.weak = weak;
    }

    /**
     * Client-only "weak" snapshot is created when server
     * sends regular canvas sync message (not a snapshot)
     */
    public static CanvasSnapshot createWeakSnapshot(byte[] colors, Long snapshotTime) {
        return new CanvasSnapshot(RANDOM.nextInt(), colors, snapshotTime, true);
    }

    /**
     * Client-only regular snapshot sent over the network,
     * made from server snapshot copy
     * @param id
     * @param colors
     * @param snapshotTime
     * @return
     */
    public static CanvasSnapshot createNetworkSnapshot(int id, byte[] colors, Long snapshotTime) {
        return new CanvasSnapshot(id, colors, snapshotTime, false);
    }

    /**
     * We're creating snapshots with the timestamp at which we
     * are sure no actions will be added, and on initial sync
     * @param colors
     * @return
     */
    public static CanvasSnapshot createServerSnapshot(byte[] colors, long timestamp) {
        return new CanvasSnapshot(RANDOM.nextInt(), colors, timestamp, false);
    }
}
