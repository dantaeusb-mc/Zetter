package me.dantaeusb.zetter.storage;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.core.Helper;
import me.dantaeusb.zetter.capability.canvastracker.CanvasServerTracker;
import me.dantaeusb.zetter.core.ZetterCanvasTypes;
import me.dantaeusb.zetter.menu.artisttable.CanvasCombinationAction;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.Tuple;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;

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

    protected CanvasData(String canvasCode) {
        super(canvasCode);
    }

    public boolean isRenderable() {
        return true;
    }

    public boolean isEditable() {
        return this.managed;
    }

    public CanvasDataType<? extends CanvasData> getType() {
        return ZetterCanvasTypes.CANVAS.get();
    }

    @Override
    public void load(CompoundNBT compoundTag) {
        this.width = compoundTag.getInt(NBT_TAG_WIDTH);
        this.height = compoundTag.getInt(NBT_TAG_HEIGHT);

        if (compoundTag.contains(NBT_TAG_RESOLUTION)) {
            int resolutionOrdinal = compoundTag.getInt(NBT_TAG_RESOLUTION);
            this.resolution = Resolution.values()[resolutionOrdinal];
        } else {
            this.resolution = Helper.getResolution();
        }

        this.updateColorData(compoundTag.getByteArray(NBT_TAG_COLOR));
    }

    public CompoundNBT save(CompoundNBT compoundTag) {
        return super.save(compoundTag);
    }

    private static class CanvasCanvasDataBuilder implements CanvasDataBuilder<CanvasData> {
        @Override
        public CanvasData supply(String canvasCode) {
            return new CanvasData(canvasCode);
        }

        /**
         * Create empty canvas data filled with canvas color
         * @param resolution
         * @param width
         * @param height
         * @return
         */
        public CanvasData createFresh(String canvasCode, Resolution resolution, int width, int height) {
            byte[] color = new byte[width * height * 4];
            ByteBuffer defaultColorBuffer = ByteBuffer.wrap(color);

            for (int x = 0; x < width * height; x++) {
                defaultColorBuffer.putInt(x * 4, Helper.CANVAS_COLOR);
            }

            final CanvasData newCanvas = new CanvasData(canvasCode);
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
        public CanvasData createWrap(String canvasCode, Resolution resolution, int width, int height, byte[] color) {
            final CanvasData newCanvas = new CanvasData(canvasCode);
            newCanvas.wrapData(resolution, width, height, color);

            return newCanvas;
        }

        /*
         * Networking
         */

        public CanvasData readPacketData(PacketBuffer networkBuffer) {
            final String canvasCode = networkBuffer.readUtf(Helper.CANVAS_CODE_MAX_LENGTH);

            final CanvasData newCanvas = new CanvasData(canvasCode);

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

        public void writePacketData(String canvasCode, CanvasData canvasData, PacketBuffer networkBuffer) {
            networkBuffer.writeUtf(canvasCode, Helper.CANVAS_CODE_MAX_LENGTH);
            networkBuffer.writeByte(canvasData.resolution.ordinal());
            networkBuffer.writeInt(canvasData.width);
            networkBuffer.writeInt(canvasData.height);
            networkBuffer.writeInt(canvasData.getColorDataBuffer().remaining());
            networkBuffer.writeBytes(canvasData.getColorDataBuffer());
        }
    }
}

