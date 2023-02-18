package me.dantaeusb.zetter.capability.canvastracker;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;

import javax.annotation.Nullable;
import java.util.BitSet;

public class CanvasTrackerStorage implements Capability.IStorage<CanvasTracker> {
    private static final String NBT_TAG_CANVAS_LAST_ID = "LastCanvasId";
    private static final String NBT_TAG_CANVAS_IDS = "CanvasIds";
    private static final String NBT_TAG_PAINTING_LAST_ID = "LastPaintingId";


    @Override
    public INBT writeNBT(Capability<CanvasTracker> capability, CanvasTracker instance, @Nullable Direction side) {
        CompoundNBT compound = new CompoundNBT();

        compound.putByteArray(NBT_TAG_CANVAS_IDS, instance.getCanvasIds().toByteArray());
        compound.putInt(NBT_TAG_CANVAS_LAST_ID, instance.getLastCanvasId());
        compound.putInt(NBT_TAG_PAINTING_LAST_ID, instance.getLastPaintingId());

        return compound;
    }

    @Override
    public void readNBT(Capability<CanvasTracker> capability, CanvasTracker instance, Direction side, @Nullable INBT tag) {
        if (tag.getType() == CompoundNBT.TYPE) {
            CompoundNBT compoundTag = (CompoundNBT) tag;

            // Backward compat for pre-16
            if (compoundTag.contains(NBT_TAG_CANVAS_IDS)) {
                byte[] canvasIds = compoundTag.getByteArray(NBT_TAG_CANVAS_IDS);

                instance.setCanvasIds(BitSet.valueOf(canvasIds));
            } else if (compoundTag.contains(NBT_TAG_CANVAS_LAST_ID)) {
                int lastCanvasId = compoundTag.getInt(NBT_TAG_CANVAS_LAST_ID);
                BitSet canvasIds = new BitSet(lastCanvasId + 1);
                canvasIds.flip(0, lastCanvasId + 1);

                instance.setCanvasIds(canvasIds);
            } else {
                instance.setCanvasIds(new BitSet());
            }

            instance.setLastPaintingId(compoundTag.getInt(NBT_TAG_PAINTING_LAST_ID));
        }
    }
}
