package me.dantaeusb.zetter.storage;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.capability.canvastracker.CanvasServerTracker;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.WorldSavedData;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

/**
 * It's not enough to just init data, we need to register it with
 * @see CanvasServerTracker ::registerCanvasData();
 */
public abstract class AbstractCanvasData extends WorldSavedData {
    public static final String NBT_TAG_TYPE = "CanvasDataType";
    @Deprecated
    public static final String NBT_TAG_TYPE_DEPRECATED = "type";

    protected static final String NBT_TAG_WIDTH = "width";
    protected static final String NBT_TAG_HEIGHT = "height";
    protected static final String NBT_TAG_RESOLUTION = "resolution";
    protected static final String NBT_TAG_COLOR = "color";

    // Maybe final?

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

    public AbstractCanvasData(String canvasCode) {
        super(canvasCode);
    }

    /**
     * Returns type of this canvas, which can return
     * resource id
     * @return
     */
    public abstract CanvasDataType<? extends AbstractCanvasData> getType();

    /**
     *
     * @param resolution
     * @param width
     * @param height
     * @param color
     */
    public final void wrapData(Resolution resolution, int width, int height, byte[] color) {
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

        this.color = color.clone();
        this.canvasBuffer = ByteBuffer.wrap(this.color);
        this.canvasBuffer.order(ByteOrder.BIG_ENDIAN);
    }

    public final boolean updateCanvasPixel(int index, int color) {
        if (!this.isEditable()) {
            Zetter.LOG.warn("Tried to update sealed canvas");
            return false;
        }

        this.canvasBuffer.putInt(index * 4, color);
        this.setDirty();
        return true;
    }

    public final int getColorAt(int pixelX, int pixelY) {
        return this.getColorAt(this.getPixelIndex(pixelX, pixelY));
    }

    public void setManaged(boolean managed) {
        this.managed = managed;
    }

    /**
     * Is canvas managed: needs to sync over the net
     * Non-managed canvases are placeholder canvases
     * and temporary storages. They can be used on server
     * but only as an intermediate storage. Mostly
     * used on client, cannot be registered on server
     * and therefore never saved.
     *
     * @return
     */
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

    /**
     * Sometimes painting could be disabled for some specific client types
     * or for other reasons, i.e. banned. We're keeping the data but marking
     * those paintings as non-renderable.
     *
     * If painting is not renderable, use placeholder.
     * @return
     */
    abstract public boolean isRenderable();

    /**
     * If painting can be edited, altered by user. Signed paintings should not
     * be editable.
     * @return
     */
    abstract public boolean isEditable();

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
    public final int getColorAt(int index) {
        return this.canvasBuffer.getInt(index * 4);
    }

    /**
     * This is integer index, not byte index!
     * @param pixelX
     * @param pixelY
     * @return
     */
    public final int getPixelIndex(int pixelX, int pixelY) {
        pixelX = MathHelper.clamp(pixelX, 0, this.width - 1);
        pixelY = MathHelper.clamp(pixelY, 0, this.height - 1);

        return pixelY * this.width + pixelX;
    }

    /*
     * Loading and syncing
     */

    public void correctData(ServerWorld level) {
        // Do nothing
    }

    public CompoundNBT save(CompoundNBT compoundTag) {
        compoundTag.putString(NBT_TAG_TYPE, this.getType().getRegistryName().toString());
        compoundTag.putInt(NBT_TAG_WIDTH, this.width);
        compoundTag.putInt(NBT_TAG_HEIGHT, this.height);
        compoundTag.putInt(NBT_TAG_RESOLUTION, this.resolution.ordinal());
        compoundTag.putByteArray(NBT_TAG_COLOR, this.color);

        return compoundTag;
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

