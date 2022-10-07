package me.dantaeusb.zetter.storage;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.core.Helper;
import me.dantaeusb.zetter.canvastracker.CanvasServerTracker;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

/**
 * It's not enough to just init data, we need to register it with
 * @see CanvasServerTracker ::registerCanvasData();
 */
public abstract class AbstractCanvasData extends SavedData {
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

    /**
     * This flag means that client can automatically manage
     * the lifetime of the canvas data and remove it from memory
     * if it was not used for some time. This can be toggled
     * off to keep data in memory for as long as needed but
     * this data will have to be collected manually.
     */
    protected boolean managed = true;

    /**
     *
     * @param resolution
     * @param width
     * @param height
     * @param color
     */
    protected final void wrapData(Resolution resolution, int width, int height, byte[] color) {
        if (width % resolution.getNumeric() != 0 || height % resolution.getNumeric() != 0) {
            throw new IllegalArgumentException("Canvas size is not proportional to given canvas resolution");
        }

        this.resolution = resolution;
        this.width = width;
        this.height = height;
        this.updateColorData(color);
        this.setDirty();
    }

    public void updateColorData(byte[] color) {
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
        this.setDirty();
        return true;
    }

    public int getColorAt(int pixelX, int pixelY) {
        return this.getColorAt(this.getPixelIndex(pixelX, pixelY));
    }

    public void setManaged(boolean managed) {
        this.managed = managed;
    }

    public boolean isManaged() {
        return this.managed;
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

    public byte[] getColorData() {
        return this.color.clone();
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
        pixelX = Mth.clamp(pixelX, 0, this.width - 1);
        pixelY = Mth.clamp(pixelY, 0, this.height - 1);

        return pixelY * this.width + pixelX;
    }

    /**
     * reads in data from the NBTTagCompound into this MapDataBase
     */
    public void load(CompoundTag compoundTag) {
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

    public CompoundTag save(CompoundTag compoundTag) {
        compoundTag.putInt(NBT_TAG_TYPE, this.getType().ordinal());
        compoundTag.putInt(NBT_TAG_WIDTH, this.width);
        compoundTag.putInt(NBT_TAG_HEIGHT, this.height);
        compoundTag.putInt(NBT_TAG_RESOLUTION, this.resolution.ordinal());
        compoundTag.putByteArray(NBT_TAG_COLOR, this.color);

        return compoundTag;
    }

    public enum Type {
        DUMMY(0),
        CANVAS(1),
        PAINTING(2);

        private static final Map<Integer, Type> lookup = new HashMap<>();

        static {
            for (Type type : Type.values()) {
                lookup.put(type.getId(), type);
            }
        }

        private final int id;

        Type(int id) {
            this.id = id;
        }

        public int getId() {
            return this.id;
        }

        public static @Nullable Type getTypeByCanvas(AbstractCanvasData abstractCanvasData) {
            if (abstractCanvasData instanceof CanvasData) {
                return Type.CANVAS;
            } else if (abstractCanvasData instanceof PaintingData) {
                return Type.PAINTING;
            } else if (abstractCanvasData instanceof DummyCanvasData) {
                return Type.DUMMY;
            }

            Zetter.LOG.warn("Cannot get type of the canvas!");
            return null;
        }

        @Nullable
        public static Type getTypeById(int id) {
            if (!lookup.containsKey(id)) {
                return null;
            }

            return lookup.get(id);
        }
    }

    public enum Resolution {
        x16(16),
        x32(32),
        x64(64);
        private static final Map<Integer, Resolution> lookup = new HashMap<>();

        static {
            for (Resolution resolution : Resolution.values()) {
                lookup.put(resolution.getNumeric(), resolution);
            }
        }

        private final int numeric;

        Resolution(int numeric) {
            this.numeric = numeric;
        }

        public int getNumeric() {
            return this.numeric;
        }

        @Nullable
        public static Resolution get(int numeric) {
            if (!lookup.containsKey(numeric)) {
                return null;
            }

            return lookup.get(numeric);
        }
    }
}

