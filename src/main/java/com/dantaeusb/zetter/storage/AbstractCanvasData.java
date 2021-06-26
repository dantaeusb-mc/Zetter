package com.dantaeusb.zetter.storage;

import com.dantaeusb.zetter.Zetter;
import com.dantaeusb.zetter.core.Helper;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.storage.WorldSavedData;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * It's not enough to just init data, we need to register it with
 * @see com.dantaeusb.zetter.canvastracker.CanvasServerTracker::registerCanvasData();
 */
public abstract class AbstractCanvasData extends WorldSavedData {
    protected static final String NBT_TAG_TYPE = "type";
    protected static final String NBT_TAG_WIDTH = "width";
    protected static final String NBT_TAG_HEIGHT = "height";
    protected static final String NBT_TAG_RESOLUTION = "resolution";
    protected static final String NBT_TAG_COLOR = "color";

    protected byte[] color;
    protected ByteBuffer canvasBuffer;

    protected Resolution resolution;
    protected int width;
    protected int height;

    public AbstractCanvasData(String canvasCode) {
        super(canvasCode);
    }

    /**
     *
     * @param resolution
     * @param width
     * @param height
     * @param color
     */
    public final void initData(Resolution resolution, int width, int height, byte[] color) {
        if (width % resolution.getNumeric() != 0 || height % resolution.getNumeric() != 0) {
            throw new IllegalArgumentException("Canvas size is not proportional to given canvas resolution");
        }

        this.resolution = resolution;
        this.width = width;
        this.height = height;
        this.updateColorData(color);
        this.markDirty();
    }

    protected void updateColorData(byte[] color) {
        if (this.color != null) {
            if (color.length != this.color.length) {
                throw new IllegalArgumentException("Color data size mismatch");
            }
        }

        this.color = color;
        this.canvasBuffer = ByteBuffer.wrap(this.color);
        this.canvasBuffer.order(ByteOrder.BIG_ENDIAN);
    }

    public boolean updateCanvasPixel(int index, int color) {
        if (!this.isEditable()) {
            Zetter.LOG.warn("Tried to update sealed canvas " + this);
            return false;
        }

        this.canvasBuffer.putInt(index * 4, color);
        this.markDirty();
        return true;
    }

    public int getColorAt(int pixelX, int pixelY) {
        return this.getColorAt(this.getPixelIndex(pixelX, pixelY));
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    public Resolution getResolution() {
        return this.resolution;
    }

    abstract public boolean isEditable();

    abstract public Type getType();

    public ByteBuffer getColorDataBuffer() {
        this.canvasBuffer.rewind();
        return this.canvasBuffer.asReadOnlyBuffer();
    }

    /**
     *
     * @param index Integer index, not byte index
     * @return
     */
    public int getColorAt(int index) {
        return this.canvasBuffer.getInt(index * 4);
    }

    /**
     * This is integer index, not byte index!
     * @param pixelX
     * @param pixelY
     * @return
     */
    public int getPixelIndex(int pixelX, int pixelY) {
        pixelX = MathHelper.clamp(pixelX, 0, this.width - 1);
        pixelY = MathHelper.clamp(pixelY, 0, this.height - 1);

        return pixelY * this.width + pixelX;
    }

    /**
     * reads in data from the NBTTagCompound into this MapDataBase
     */
    public void read(CompoundNBT compound) {
        this.width = compound.getInt(NBT_TAG_WIDTH);
        this.height = compound.getInt(NBT_TAG_HEIGHT);

        if (compound.contains(NBT_TAG_RESOLUTION)) {
            int resolutionOrdinal = compound.getInt(NBT_TAG_RESOLUTION);
            this.resolution = Resolution.values()[resolutionOrdinal];
        } else {
            this.resolution = Helper.getResolution();
        }

        this.updateColorData(compound.getByteArray(NBT_TAG_COLOR));
    }

    public CompoundNBT write(CompoundNBT compound) {
        compound.putInt(NBT_TAG_TYPE, this.getType().ordinal());
        compound.putInt(NBT_TAG_WIDTH, this.width);
        compound.putInt(NBT_TAG_HEIGHT, this.height);
        compound.putInt(NBT_TAG_RESOLUTION, this.resolution.ordinal());
        compound.putByteArray(NBT_TAG_COLOR, this.color);

        return compound;
    }

    public enum Type {
        DUMMY,
        CANVAS,
        PAINTING
    }

    public enum Resolution {
        x16(16),
        x32(32),
        x64(64);

        private final int numeric;

        Resolution(int numeric) {
            this.numeric = numeric;
        }

        public int getNumeric() {
            return numeric;
        }
    }
}

