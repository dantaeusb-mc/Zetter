package me.dantaeusb.zetter.storage;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.core.Helper;
import me.dantaeusb.zetter.canvastracker.CanvasServerTracker;
import net.minecraft.nbt.CompoundTag;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * It's not enough to just init data, we need to register it with
 * @see CanvasServerTracker ::registerCanvasData();
 */
public class DummyCanvasData extends AbstractCanvasData {
    protected DummyCanvasData() {
        super();
    }

    public static DummyCanvasData createDummy() {
        int width = Helper.getResolution().getNumeric();
        int height = Helper.getResolution().getNumeric();
        Resolution resolution = Helper.getResolution();

        return DummyCanvasData.createDummy(width, height, resolution);
    }

    public static DummyCanvasData createDummy(int width, int height, Resolution resolution) {

        byte[] color = new byte[width * height * 4];
        ByteBuffer defaultColorBuffer = ByteBuffer.wrap(color);

        final int halfWidth = width / 2;
        final int halfHeight = width / 2;

        for (int x = 0; x < width * height; x++) {
            defaultColorBuffer.putInt(x * 4, width > halfWidth ^ height > halfHeight ? Helper.DUMMY_PINK_COLOR : Helper.DUMMY_BLACK_COLOR);
        }

        final DummyCanvasData newDummyCanvas = new DummyCanvasData();
        newDummyCanvas.wrapData(resolution, width, height, color);

        return newDummyCanvas;
    }

    public static DummyCanvasData createWrap(Resolution resolution, int width, int height, byte[] color) {
        final DummyCanvasData newCanvas = new DummyCanvasData();
        newCanvas.wrapData(resolution, width, height, color);

        return newCanvas;
    }

    protected void updateColorData(byte[] color) {
        // Don't check size mismatch cause we might use it as combined canvas

        this.color = color;
        this.canvasBuffer = ByteBuffer.wrap(this.color);
        this.canvasBuffer.order(ByteOrder.BIG_ENDIAN);
    }

    public boolean isEditable() {
        return false;
    }

    public Type getType() {
        return Type.DUMMY;
    }

    public void load(CompoundTag compoundTag) {
        Zetter.LOG.error("Trying to read into dummy canvas!");
    }

    public CompoundTag save(CompoundTag compoundTag) {
        Zetter.LOG.error("Trying to save dummy canvas!");

        return compoundTag;
    }
}

