package me.dantaeusb.zetter.canvastracker;

import java.util.BitSet;
import me.dantaeusb.zetter.storage.AbstractCanvasData;
import me.dantaeusb.zetter.storage.DummyCanvasData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

public class CanvasDefaultTracker implements ICanvasTracker {
    public CanvasDefaultTracker() {
        //this.registerCanvasData(Helper.FALLBACK_CANVAS_CODE, DummyCanvasData.createDummy());
    }

    public Level getWorld() {
        return null;
    }

    /*
     * Helper methods
     */

    @Nullable
    public <T extends AbstractCanvasData> T getCanvasData(String canvasCode, @Nullable Class<T> type) {
        return (T) DummyCanvasData.createDummy();
    }

    public void registerCanvasData(String canvasCode, AbstractCanvasData canvasData) {

    }

    public void unregisterCanvasData(String canvasCode) {

    }
}
