package me.dantaeusb.zetter.storage;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.core.Helper;
import me.dantaeusb.zetter.capability.canvastracker.CanvasServerTracker;
import me.dantaeusb.zetter.core.ZetterCanvasTypes;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * It's not enough to just init data, we need to register it with
 * @see CanvasServerTracker ::registerCanvasData();
 *
 * It has Dummy type without prefix, it should not be saved
 */
public class DummyCanvasData extends AbstractCanvasData {
    public static final String TYPE = "dummy";
    public static final String CODE_PREFIX = Zetter.MOD_ID + "_" + TYPE + "_";

    public static final CanvasDataBuilder<DummyCanvasData> BUILDER = new DummyCanvasDataBuilder();

    protected DummyCanvasData(String canvasCode) {
        super(canvasCode);
    }

    public static DummyCanvasData createDummy(String canvasCode) {
        int width = Helper.getResolution().getNumeric();
        int height = Helper.getResolution().getNumeric();
        Resolution resolution = Helper.getResolution();

        return DummyCanvasData.createDummy(canvasCode, resolution, width, height);
    }

    public static DummyCanvasData createDummy(String canvasCode, Resolution resolution, int width, int height) {
        byte[] color = new byte[width * height * 4];
        ByteBuffer defaultColorBuffer = ByteBuffer.wrap(color);

        final int halfWidth = width / 2;
        final int halfHeight = width / 2;

        for (int x = 0; x < width * height; x++) {
            defaultColorBuffer.putInt(x * 4, width > halfWidth ^ height > halfHeight ? Helper.DUMMY_PINK_COLOR : Helper.DUMMY_BLACK_COLOR);
        }

        final DummyCanvasData newDummyCanvas = new DummyCanvasData(canvasCode);
        newDummyCanvas.wrapData(resolution, width, height, color);

        return newDummyCanvas;
    }

    public void updateColorData(byte[] color) {
        // Don't check size mismatch cause we might use it as combined canvas

        this.color = color;
        this.canvasBuffer = ByteBuffer.wrap(this.color);
        this.canvasBuffer.order(ByteOrder.BIG_ENDIAN);
    }

    // All dummy canvases are not managed, they are not synced over the net and manually destroyed
    public boolean isManaged() {
        return false;
    }

    public boolean isRenderable() {
        return true;
    }

    public boolean isEditable() {
        return false;
    }

    @Override
    public void load(CompoundNBT compoundTag) {
        Zetter.LOG.error("Trying to read into dummy canvas!");
    }

    public CanvasDataType<DummyCanvasData> getType() {
        return ZetterCanvasTypes.DUMMY.get();
    }

    private static class DummyCanvasDataBuilder implements CanvasDataBuilder<DummyCanvasData> {
        @Override
        public DummyCanvasData supply(String canvasCode) {
            return new DummyCanvasData(canvasCode);
        }

        @Override
        public DummyCanvasData createFresh(String canvasCode, Resolution resolution, int width, int height) {
            return DummyCanvasData.createDummy(canvasCode, resolution, width, height);
        }

        @Override
        public DummyCanvasData createWrap(String canvasCode, Resolution resolution, int width, int height, byte[] color) {
            final DummyCanvasData newCanvas = new DummyCanvasData(canvasCode);
            newCanvas.wrapData(resolution, width, height, color);

            return newCanvas;
        }

        @Override
        public DummyCanvasData readPacketData(PacketBuffer networkBuffer) {
            throw new IllegalStateException("Trying to read Dummy Canvas from network!");
        }

        @Override
        public void writePacketData(String canvasCode, DummyCanvasData canvasData, PacketBuffer networkBuffer) {
            throw new IllegalStateException("Trying to write Dummy Canvas to network!");
        }
    }
}

