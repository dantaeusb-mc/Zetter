package me.dantaeusb.zetter.storage;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.canvastracker.CanvasServerTracker;
import me.dantaeusb.zetter.core.Helper;
import me.dantaeusb.zetter.core.ZetterCanvasTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

import java.nio.ByteBuffer;

/**
 * It's not enough to just init data, we need to register it with
 * @see CanvasServerTracker ::registerCanvasData();
 */
public class PaintingData extends AbstractCanvasData {
    public static final String TYPE = "painting";
    public static final String CODE_PREFIX = Zetter.MOD_ID + "_" + TYPE + "_";

    protected static final String NBT_TAG_AUTHOR_NAME = "author_name";
    protected static final String NBT_TAG_TITLE = "title";
    protected static final String NBT_TAG_BANNED = "Banned";

    public static final int MAX_GENERATION = 2;

    protected String authorName;
    protected String title;
    protected boolean banned;

    protected PaintingData() {
        super(Zetter.MOD_ID, TYPE);

        this.banned = false;
    }

    /**
     * @todo: [HIGH] Use placeholders
     * @param resolution
     * @param width
     * @param height
     * @return
     */
    public static PaintingData createFresh(Resolution resolution, int width, int height) {
        final PaintingData newPainting = new PaintingData();

        byte[] color = new byte[width * height * 4];
        ByteBuffer defaultColorBuffer = ByteBuffer.wrap(color);

        for (int x = 0; x < width * height; x++) {
            defaultColorBuffer.putInt(x * 4, Helper.CANVAS_COLOR);
        }

        newPainting.wrapData(resolution, width, height, color);

        return newPainting;
    }

    public static PaintingData createWrap(Resolution resolution, int width, int height, byte[] color) {
        final PaintingData newPainting = new PaintingData();
        newPainting.wrapData(resolution, width, height, color);

        return newPainting;
    }

    public static String getCanvasCode(int canvasId) {
        return CODE_PREFIX + canvasId;
    }

    public void setMetaProperties(String authorName, String title) {
        this.authorName = authorName;
        this.title = title;
    }

    public String getPaintingTitle() {
        return this.title;
    }

    public String getAuthorName() {
        return this.authorName;
    }

    public static String getPaintingCode(int paintingId) {
        return CODE_PREFIX + paintingId;
    }

    public boolean isRenderable() {
        return true;
    }

    public boolean isEditable() {
        return false;
    }

    public CanvasDataType<PaintingData> getType() {
        return ZetterCanvasTypes.PAINTING.get();
    }

    /*
     * Serialization
     */

    public static PaintingData load(CompoundTag compoundTag) {
        final PaintingData newPainting = new PaintingData();

        newPainting.width = compoundTag.getInt(NBT_TAG_WIDTH);
        newPainting.height = compoundTag.getInt(NBT_TAG_HEIGHT);

        if (compoundTag.contains(NBT_TAG_RESOLUTION)) {
            int resolutionOrdinal = compoundTag.getInt(NBT_TAG_RESOLUTION);
            newPainting.resolution = Resolution.values()[resolutionOrdinal];
        } else {
            newPainting.resolution = Helper.getResolution();
        }

        newPainting.updateColorData(compoundTag.getByteArray(NBT_TAG_COLOR));

        newPainting.authorName = compoundTag.getString(NBT_TAG_AUTHOR_NAME);
        newPainting.title = compoundTag.getString(NBT_TAG_TITLE);
        newPainting.banned = compoundTag.getBoolean(NBT_TAG_BANNED);

        return newPainting;
    }

    public CompoundTag save(CompoundTag compoundTag) {
        super.save(compoundTag);

        compoundTag.putString(NBT_TAG_AUTHOR_NAME, this.authorName);
        compoundTag.putString(NBT_TAG_TITLE, this.title);
        compoundTag.putBoolean(NBT_TAG_BANNED, this.banned);

        return compoundTag;
    }

    /*
     * Networking
     */

    public static PaintingData readPacketData(FriendlyByteBuf networkBuffer) {
        final PaintingData newPainting = new PaintingData();

        final byte resolutionOrdinal = networkBuffer.readByte();
        AbstractCanvasData.Resolution resolution = AbstractCanvasData.Resolution.values()[resolutionOrdinal];

        final int width = networkBuffer.readInt();
        final int height = networkBuffer.readInt();

        final int colorDataSize = networkBuffer.readInt();
        ByteBuffer colorData = networkBuffer.readBytes(colorDataSize).nioBuffer();
        byte[] unwrappedColorData = new byte[width * height * 4];
        colorData.get(unwrappedColorData);

        newPainting.wrapData(
                resolution,
                width,
                height,
                unwrappedColorData
        );

        final String authorName = networkBuffer.readUtf(64);
        final String title = networkBuffer.readUtf(32);

        newPainting.setMetaProperties(
                authorName,
                title
        );

        return newPainting;
    }

    public static void writePacketData(PaintingData canvasData, FriendlyByteBuf networkBuffer) {
        networkBuffer.writeByte(canvasData.resolution.ordinal());
        networkBuffer.writeInt(canvasData.width);
        networkBuffer.writeInt(canvasData.height);
        networkBuffer.writeInt(canvasData.getColorDataBuffer().remaining());
        networkBuffer.writeBytes(canvasData.getColorDataBuffer());
        networkBuffer.writeUtf(canvasData.authorName, 64);
        networkBuffer.writeUtf(canvasData.title, 32);
    }
}

