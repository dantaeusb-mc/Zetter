package com.dantaeusb.zetter.canvastracker;

import com.dantaeusb.zetter.storage.AbstractCanvasData;
import com.dantaeusb.zetter.storage.DummyCanvasData;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.util.Direction;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;

import javax.annotation.Nullable;

public class CanvasDefaultTracker implements ICanvasTracker  {
    public World getWorld() {
        return null;
    }

    public int getNextCanvasId() {
        return 0;
    }

    public int getLastCanvasId() {
        return 0;
    }

    public void setLastCanvasId(int id) {}

    public int getNextPaintingId() {
        return 0;
    }

    public int getLastPaintingId() {
        return 0;
    }

    public void setLastPaintingId(int id) {}

    @Nullable
    public <T extends AbstractCanvasData> T getCanvasData(String canvasCode, @Nullable Class<T> type) {
        return (T) new DummyCanvasData();
    }

    public void registerCanvasData(AbstractCanvasData canvasData) {}

    // Convert to/from NBT
    static class CanvasTrackerNBTStorage implements Capability.IStorage<CanvasDefaultTracker> {
        @Override
        public INBT writeNBT(Capability<CanvasDefaultTracker> capability, CanvasDefaultTracker instance, @Nullable Direction side) {
            CompoundNBT compound = new CompoundNBT();
            compound.putInt(NBT_TAG_LAST_CANVAS_ID, instance.getLastCanvasId());
            compound.putInt(NBT_TAG_LAST_PAINTING_ID, instance.getLastPaintingId());
            return compound;
        }

        @Override
        public void readNBT(Capability<CanvasDefaultTracker> capability, CanvasDefaultTracker instance, Direction side, @Nullable INBT nbt) {
            instance.setLastCanvasId(0);

            if (nbt.getType() == CompoundNBT.TYPE) {
                CompoundNBT castedNBT = (CompoundNBT) nbt;
                instance.setLastCanvasId(castedNBT.getInt(NBT_TAG_LAST_CANVAS_ID));
                instance.setLastPaintingId(castedNBT.getInt(NBT_TAG_LAST_PAINTING_ID));
            }
        }
    }
}
