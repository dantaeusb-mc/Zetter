package com.dantaeusb.zetter.canvastracker;

import com.dantaeusb.zetter.storage.CanvasData;
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

    public int getNextId() {
        return 0;
    }

    public int getLastId() {
        return 0;
    }

    public void setLastId(int id) {}

    @Nullable
    public CanvasData getCanvasData(String canvasName) {
        return new CanvasData(0);
    }

    public void registerCanvasData(CanvasData canvasData) {}

    // Convert to/from NBT
    static class CanvasTrackerNBTStorage implements Capability.IStorage<CanvasDefaultTracker> {
        @Override
        public INBT writeNBT(Capability<CanvasDefaultTracker> capability, CanvasDefaultTracker instance, @Nullable Direction side) {
            CompoundNBT compound = new CompoundNBT();
            compound.putInt(NBT_TAG_LAST_ID, instance.getLastId());
            return compound;
        }

        @Override
        public void readNBT(Capability<CanvasDefaultTracker> capability, CanvasDefaultTracker instance, Direction side, @Nullable INBT nbt) {
            instance.setLastId(0);

            if (nbt.getType() == CompoundNBT.TYPE) {
                CompoundNBT castedNBT = (CompoundNBT) nbt;
                instance.setLastId(castedNBT.getInt(NBT_TAG_LAST_ID));
            }
        }
    }
}
