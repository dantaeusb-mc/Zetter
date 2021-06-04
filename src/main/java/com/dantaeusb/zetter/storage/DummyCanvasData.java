package com.dantaeusb.zetter.storage;

import com.dantaeusb.zetter.Zetter;
import net.minecraft.nbt.CompoundNBT;

/**
 * It's not enough to just init data, we need to register it with
 * @see com.dantaeusb.zetter.canvastracker.CanvasServerTracker::registerCanvasData();
 */
public class DummyCanvasData extends AbstractCanvasData {
    public DummyCanvasData() {
        super(Zetter.MOD_ID + "_dummy");
    }

    public DummyCanvasData(String name) {
        super(name);
    }

    public boolean isEditable() {
        return false;
    }

    public Type getType() {
        return Type.DUMMY;
    }

    public void read(CompoundNBT compound) {
        Zetter.LOG.error("Trying to read into dummy canvas!");
    }

    public CompoundNBT write(CompoundNBT compound) {
        Zetter.LOG.error("Trying to save dummy canvas!");

        return compound;
    }
}

