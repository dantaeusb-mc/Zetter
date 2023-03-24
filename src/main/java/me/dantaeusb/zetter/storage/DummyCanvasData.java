package me.dantaeusb.zetter.storage;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.core.Helper;
import me.dantaeusb.zetter.capability.canvastracker.CanvasServerTracker;
import me.dantaeusb.zetter.core.ZetterCanvasTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * It's not enough to just init data, we need to register it with
 *
 * @see CanvasServerTracker ::registerCanvasData();
 * <p>
 * It has Dummy type without prefix, it should not be saved
 */
public class DummyCanvasData extends AbstractCanvasData {
    public static final String TYPE = "dummy";
    public static final String CODE_PREFIX = Zetter.MOD_ID + "_" + TYPE + "_";

    public static final CanvasDataBuilder<DummyCanvasData> BUILDER = new DummyCanvasDataBuilder();

    protected DummyCanvasData() {
    }

    public static DummyCanvasData createDummy() {
        int width = Helper.getResolution().getNumeric();
        int height = Helper.getResolution().getNumeric();
        Resolution resolution = Helper.getResolution();

        return DummyCanvasData.createDummy(resolution, width, height);
    }

    public static DummyCanvasData createDummy(Resolution resolution, int width, int height) {

        byte[] color = new byte[width * height * 4];
        ByteBuffer defaultColorBuffer = ByteBuffer.wrap(color);

        final int halfResolution = resolution.getNumeric() / 2;

        for (int x = 0; x < width * height; x++) {
            defaultColorBuffer.putInt(
                x * 4,
                ((x / width) % resolution.getNumeric() >= halfResolution ?
                    ((x % resolution.getNumeric()) < halfResolution ?
                        Helper.DUMMY_PINK_COLOR :
                        Helper.DUMMY_BLACK_COLOR
                    ) :
                    ((x % resolution.getNumeric()) < halfResolution ?
                        Helper.DUMMY_BLACK_COLOR :
                        Helper.DUMMY_PINK_COLOR
                    )
                )
            );
        }

        final DummyCanvasData newDummyCanvas = new DummyCanvasData();
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

    public CanvasDataType<DummyCanvasData> getType() {
        return ZetterCanvasTypes.DUMMY.get();
    }

    private static class DummyCanvasDataBuilder implements CanvasDataBuilder<DummyCanvasData> {
        public DummyCanvasData createFresh(Resolution resolution, int width, int height) {
            return DummyCanvasData.createDummy(resolution, width, height);
        }

        public DummyCanvasData createWrap(Resolution resolution, int width, int height, byte[] color) {
            final DummyCanvasData newCanvas = new DummyCanvasData();
            newCanvas.wrapData(resolution, width, height, color);

            return newCanvas;
        }

        /**
         * @param compoundTag
         * @return
         * @todo: [HIGH] Use a placeholder, it's fall-back
         */
        public DummyCanvasData load(CompoundTag compoundTag) {
            Zetter.LOG.error("Trying to read into dummy canvas!");

            return DummyCanvasData.createDummy();
        }

        public CompoundTag save(CompoundTag compoundTag) {
            Zetter.LOG.error("Trying to save dummy canvas!");

            return compoundTag;
        }

        public DummyCanvasData readPacketData(FriendlyByteBuf networkBuffer) {
            throw new IllegalStateException("Trying to read Dummy Canvas from network!");
        }

        public void writePacketData(DummyCanvasData canvasData, FriendlyByteBuf networkBuffer) {
            throw new IllegalStateException("Trying to write Dummy Canvas to network!");
        }
    }
}

