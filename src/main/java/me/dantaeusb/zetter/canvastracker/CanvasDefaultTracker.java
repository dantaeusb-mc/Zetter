package me.dantaeusb.zetter.canvastracker;

import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import me.dantaeusb.zetter.storage.AbstractCanvasData;
import me.dantaeusb.zetter.storage.DummyCanvasData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.RegistryObject;

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
    public <T extends AbstractCanvasData> T getCanvasData(String canvasCode) {
        return (T) DummyCanvasData.createDummy();
    }

    public void registerCanvasData(String canvasCode, AbstractCanvasData canvasData) {

    }

    public void unregisterCanvasData(String canvasCode) {

    }
}
