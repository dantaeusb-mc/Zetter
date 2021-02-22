package com.dantaeusb.zetter.storage;

import com.dantaeusb.zetter.Zetter;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.storage.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class CanvasData extends WorldSavedData {
    private byte[] color;
    private ByteBuffer canvasBuffer;

    /**
     * @todo: add work with sealed attribute to avoid unnecessary tracking and server communication
     * Sealed canvas cannot be modified
     */
    private boolean isSealed = false;

    private int width;
    private int height;

    public static final String NAME_PREFIX = Zetter.MOD_ID + "_canvas_";

    private static final String NBT_TAG_NAME_SEALED = "sealed";
    private static final String NBT_TAG_NAME_WIDTH = "width";
    private static final String NBT_TAG_NAME_HEIGHT = "height";
    private static final String NBT_TAG_NAME_COLOR = "color";

    public CanvasData(String canvasName) {
        super(canvasName);
    }

    public CanvasData(int canvasId) {
        super(getCanvasName(canvasId));
    }

    public static String getCanvasName(int canvasId) {
        return NAME_PREFIX + canvasId;
    }

    public void initData(int width, int height) {
        byte[] defaultColor = new byte[width * height * 4];
        ByteBuffer defaultColorBuffer = ByteBuffer.wrap(defaultColor);

        for (int x = 0; x < width * height; x++) {
            defaultColorBuffer.putInt(x * 4, 0xFFE0DACE);
        }

        this.initData(width, height, defaultColor);
    }

    public void initData(int width, int height, byte[] color) {
        this.width = width;
        this.height = height;
        this.updateColorData(color);
        this.markDirty();
    }

    public void copyFrom(CanvasData templateCanvasData) {
        if (this.isSealed) {
            Zetter.LOG.error("Cannot copy to sealed canvas");
            return;
        }

        this.width = templateCanvasData.getWidth();
        this.height = templateCanvasData.getHeight();
        this.updateColorData(templateCanvasData.color);
        this.markDirty();
    }

    private void updateColorData(byte[] color) {
        this.color = color;
        this.canvasBuffer = ByteBuffer.wrap(this.color);
        this.canvasBuffer.order(ByteOrder.BIG_ENDIAN);
    }

    public void updateCanvasPixel(int index, int color) {
        if (this.isSealed) {
            Zetter.LOG.warn("Tried to update sealed canvas " + this);
        }

        this.canvasBuffer.putInt(index * 4, color);
        this.markDirty();
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

    public boolean isSealed() {
        return this.isSealed;
    }

    public void seal() {
        this.isSealed = true;
    }

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
        this.isSealed = compound.getBoolean(NBT_TAG_NAME_SEALED);
        this.width = compound.getInt(NBT_TAG_NAME_WIDTH);
        this.height = compound.getInt(NBT_TAG_NAME_HEIGHT);
        this.updateColorData(compound.getByteArray(NBT_TAG_NAME_COLOR));
    }

    public CompoundNBT write(CompoundNBT compound) {
        compound.putBoolean(NBT_TAG_NAME_SEALED, this.isSealed);
        compound.putInt(NBT_TAG_NAME_WIDTH, this.width);
        compound.putInt(NBT_TAG_NAME_HEIGHT, this.height);
        compound.putByteArray(NBT_TAG_NAME_COLOR, this.color);

        return compound;
    }
}

