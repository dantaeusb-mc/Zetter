package me.dantaeusb.zetter.entity.item.state.representation;

import java.util.UUID;

/**
 * Snapshots are complete copy of current canvas
 * at a particular time, they're created only on
 * server side but then send to client for easier
 * state restoration
 */
public class PaintingSnapshot {
    public final UUID id;
    public final Long timestamp;
    public final byte[] colors;
    public final boolean clientSide;

    public PaintingSnapshot(byte[] colors) {
        this(UUID.randomUUID(), colors, System.currentTimeMillis(), false);
    }

    public PaintingSnapshot(UUID uuid, byte[] colors, Long snapshotTime, boolean clientSide) {
        this.id = uuid;
        this.timestamp = snapshotTime;
        this.colors = colors;
        this.clientSide = clientSide;
    }
}
