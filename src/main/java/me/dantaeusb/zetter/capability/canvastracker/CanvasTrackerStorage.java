package me.dantaeusb.zetter.capability.canvastracker;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import java.util.BitSet;

public class CanvasTrackerStorage {
    private static final String NBT_TAG_CANVAS_LAST_ID = "LastCanvasId";
    private static final String NBT_TAG_CANVAS_IDS = "CanvasIds";
    private static final String NBT_TAG_PAINTING_LAST_ID = "LastPaintingId";

    public static Tag save(CanvasTracker canvasTracker) {
        CompoundTag compound = new CompoundTag();

        compound.putByteArray(NBT_TAG_CANVAS_IDS, canvasTracker.getCanvasIds().toByteArray());
        compound.putInt(NBT_TAG_CANVAS_LAST_ID, canvasTracker.getLastCanvasId());
        compound.putInt(NBT_TAG_PAINTING_LAST_ID, canvasTracker.getLastPaintingId());

        return compound;
    }

    public static void load(CanvasTracker canvasTracker, Tag tag) {
        if (tag.getType() == CompoundTag.TYPE) {
            CompoundTag compoundTag = (CompoundTag) tag;

            // Backward compat for pre-16
            if (compoundTag.contains(NBT_TAG_CANVAS_IDS)) {
                byte[] canvasIds = compoundTag.getByteArray(NBT_TAG_CANVAS_IDS);

                canvasTracker.setCanvasIds(BitSet.valueOf(canvasIds));
            } else if (compoundTag.contains(NBT_TAG_CANVAS_LAST_ID)) {
                int lastCanvasId = compoundTag.getInt(NBT_TAG_CANVAS_LAST_ID);
                BitSet canvasIds = new BitSet(lastCanvasId + 1);
                canvasIds.flip(0, lastCanvasId + 1);

                canvasTracker.setCanvasIds(canvasIds);
            } else {
                canvasTracker.setCanvasIds(new BitSet());
            }

            canvasTracker.setLastPaintingId(compoundTag.getInt(NBT_TAG_PAINTING_LAST_ID));
        }
    }
}
