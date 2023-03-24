package me.dantaeusb.zetter.storage;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.capability.canvastracker.CanvasServerTracker;
import me.dantaeusb.zetter.core.Helper;
import me.dantaeusb.zetter.core.ZetterCanvasTypes;
import me.dantaeusb.zetter.menu.artisttable.CanvasCombinationAction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

import java.nio.ByteBuffer;
import java.util.HashMap;

/**
 * It's not enough to just init data, we need to register it with
 * @see CanvasServerTracker ::registerCanvasData();
 */
public class CanvasData extends AbstractCanvasData {
    public static final String TYPE = "canvas";
    public static final String CODE_PREFIX = Zetter.MOD_ID + "_" + TYPE + "_";
    public static final CanvasDataBuilder<CanvasData> BUILDER = new CanvasCanvasDataBuilder();

    public static String getCanvasCode(int canvasId) {
        return CODE_PREFIX + canvasId;
    }

    /**
     * Default canvases indexed by size NxM
     * @param widthBlocks
     * @param heightBlocks
     * @return
     */
    public static String getDefaultCanvasCode(int widthBlocks, int heightBlocks) {
        return CODE_PREFIX + "default_" + widthBlocks + "x" + heightBlocks;
    }

    protected CanvasData() {}

    public boolean isRenderable() {
        return true;
    }

    public boolean isEditable() {
        return this.managed;
    }

    public CanvasDataType<? extends CanvasData> getType() {
        return ZetterCanvasTypes.CANVAS.get();
    }

    public CompoundTag save(CompoundTag compoundTag) {
        return super.save(compoundTag);
    }

    private static class CanvasCanvasDataBuilder implements CanvasDataBuilder<CanvasData> {
        /**
         * Create empty canvas data filled with canvas color
         * @param resolution
         * @param width
         * @param height
         * @return
         */
        public CanvasData createFresh(Resolution resolution, int width, int height) {
            byte[] color = new byte[width * height * 4];
            ByteBuffer defaultColorBuffer = ByteBuffer.wrap(color);

            for (int x = 0; x < width * height; x++) {
                defaultColorBuffer.putInt(x * 4, Helper.CANVAS_COLOR);
            }

            final CanvasData newCanvas = new CanvasData();
            newCanvas.wrapData(resolution, width, height, color);

            return newCanvas;
        }

        /**
         * Create canvas from existing data
         * @param resolution
         * @param width
         * @param height
         * @param color
         * @return
         */
        public CanvasData createWrap(Resolution resolution, int width, int height, byte[] color) {
            final CanvasData newCanvas = new CanvasData();
            newCanvas.wrapData(resolution, width, height, color);

            return newCanvas;
        }

        public CanvasData load(CompoundTag compoundTag) {
            final CanvasData newCanvas = new CanvasData();

            newCanvas.width = compoundTag.getInt(NBT_TAG_WIDTH);
            newCanvas.height = compoundTag.getInt(NBT_TAG_HEIGHT);

            if (compoundTag.contains(NBT_TAG_RESOLUTION)) {
                int resolutionOrdinal = compoundTag.getInt(NBT_TAG_RESOLUTION);
                newCanvas.resolution = Resolution.values()[resolutionOrdinal];
            } else {
                newCanvas.resolution = Helper.getResolution();
            }

            newCanvas.updateColorData(compoundTag.getByteArray(NBT_TAG_COLOR));

            return newCanvas;
        }

        /*
         * Networking
         */

        public CanvasData readPacketData(FriendlyByteBuf networkBuffer) {
            final CanvasData newCanvas = new CanvasData();

            final byte resolutionOrdinal = networkBuffer.readByte();
            AbstractCanvasData.Resolution resolution = AbstractCanvasData.Resolution.values()[resolutionOrdinal];

            final int width = networkBuffer.readInt();
            final int height = networkBuffer.readInt();

            final int colorDataSize = networkBuffer.readInt();
            ByteBuffer colorData = networkBuffer.readBytes(colorDataSize).nioBuffer();
            byte[] unwrappedColorData = new byte[width * height * 4];
            colorData.get(unwrappedColorData);

            newCanvas.wrapData(
                resolution,
                width,
                height,
                unwrappedColorData
            );

            return newCanvas;
        }

        public void writePacketData(CanvasData canvasData, FriendlyByteBuf networkBuffer) {
            networkBuffer.writeByte(canvasData.resolution.ordinal());
            networkBuffer.writeInt(canvasData.width);
            networkBuffer.writeInt(canvasData.height);
            networkBuffer.writeInt(canvasData.getColorDataBuffer().remaining());
            networkBuffer.writeBytes(canvasData.getColorDataBuffer());
        }
    }
}

