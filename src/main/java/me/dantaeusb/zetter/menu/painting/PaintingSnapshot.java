package me.dantaeusb.zetter.menu.painting;

import net.minecraft.util.Tuple;

import java.util.Stack;
import java.util.UUID;

/**
 * Snapshots are complete copy of current canvas
 * at a particular time, they're created only on
 * server side but then send to client for easier
 * state restoration
 */
public class PaintingSnapshot {
    private final Long snapshotTime;
    private final int[] colors;

    public PaintingSnapshot(Long snapshotTime, int[] colors) {
        this.snapshotTime = snapshotTime;
        this.colors = colors;
    }
}
