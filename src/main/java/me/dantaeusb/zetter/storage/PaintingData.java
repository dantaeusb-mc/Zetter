package me.dantaeusb.zetter.storage;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.capability.canvastracker.CanvasServerTracker;
import me.dantaeusb.zetter.client.gui.overlay.PaintingInfoOverlay;
import me.dantaeusb.zetter.core.Helper;
import me.dantaeusb.zetter.core.ZetterCanvasTypes;
import me.dantaeusb.zetter.core.ZetterOverlays;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.world.server.ServerWorld;

import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.UUID;

/**
 * It's not enough to just init data, we need to register it with
 * @see CanvasServerTracker ::registerCanvasData();
 */
public class PaintingData extends AbstractCanvasData {
    public static final String TYPE = "painting";
    public static final String CODE_PREFIX = Zetter.MOD_ID + "_" + TYPE + "_";

    public static final CanvasDataBuilder<PaintingData> BUILDER = new PaintingDataBuilder();

    public static final int MAX_GENERATION = 2;
    public static final UUID FALLBACK_UUID = new UUID(0L, 0L);

    protected static final String NBT_TAG_AUTHOR_NAME = "author_name";
    protected static final String NBT_TAG_AUTHOR_UUID = "AuthorUuid";
    protected static final String NBT_TAG_NAME = "title";
    protected static final String NBT_TAG_BANNED = "Banned";

    protected UUID authorUuid;
    protected String authorName;
    protected String name;
    protected boolean banned = false;

    protected PaintingData(String canvasCode) {
        super(canvasCode);
    }

    @Override
    public void load(CompoundNBT compoundTag) {
        this.width = compoundTag.getInt(NBT_TAG_WIDTH);
        this.height = compoundTag.getInt(NBT_TAG_HEIGHT);

        int resolutionOrdinal = compoundTag.getInt(NBT_TAG_RESOLUTION);
        this.resolution = Resolution.values()[resolutionOrdinal];

        this.updateColorData(compoundTag.getByteArray(NBT_TAG_COLOR));

        if (compoundTag.contains(NBT_TAG_AUTHOR_UUID)) {
            this.authorUuid = compoundTag.getUUID(NBT_TAG_AUTHOR_UUID);
        } else {
            this.authorUuid = null;
        }

        this.authorName = compoundTag.getString(NBT_TAG_AUTHOR_NAME);
        this.name = compoundTag.getString(NBT_TAG_NAME);
        this.banned = compoundTag.getBoolean(NBT_TAG_BANNED);
    }

    public static String getCanvasCode(int canvasId) {
        return CODE_PREFIX + canvasId;
    }

    public void setMetaProperties(UUID authorUuid, String authorName, String name) {
        this.authorUuid = authorUuid;
        this.authorName = authorName;
        this.name = name;
    }

    public String getPaintingName() {
        return this.name;
    }

    public UUID getAuthorUuid() {
        return this.authorUuid;
    }

    public String getAuthorName() {
        return this.authorName;
    }

    public static String getPaintingCode(int paintingId) {
        return CODE_PREFIX + paintingId;
    }

    public boolean isBanned() {
        return this.banned;
    }

    public boolean isRenderable() {
        return true;
    }

    public boolean isEditable() {
        return false;
    }

    public PaintingInfoOverlay getOverlay() {
        return ZetterOverlays.PAINTING_INFO;
    }

    public CanvasDataType<? extends PaintingData> getType() {
        return ZetterCanvasTypes.PAINTING.get();
    }

    /**
     * One day we will remove that, and for paintings created
     * long ago where we were unable to restore author id,
     * we will keep fallback UUID.
     * @param level
     */
    @Override
    public void correctData(ServerWorld level) {
        if (this.authorUuid == null || this.authorUuid.equals(FALLBACK_UUID)) {
            UUID authorUuid = Helper.tryToRestoreAuthorUuid(level, this.authorName);

            if (authorUuid != null) {
                this.authorUuid = authorUuid;
            } else {
                Zetter.LOG.warn("Cannot restore author UUID for player " + this.authorName);
                this.authorUuid = FALLBACK_UUID;
            }

            this.setDirty();
        }
    }

    public CompoundNBT save(CompoundNBT compoundTag) {
        super.save(compoundTag);

        compoundTag.putUUID(NBT_TAG_AUTHOR_UUID, this.authorUuid);
        compoundTag.putString(NBT_TAG_AUTHOR_NAME, this.authorName);
        compoundTag.putString(NBT_TAG_NAME, this.name);
        compoundTag.putBoolean(NBT_TAG_BANNED, this.banned);

        return compoundTag;
    }

    private static class PaintingDataBuilder implements CanvasDataBuilder<PaintingData> {
        @Override
        public PaintingData supply(String canvasCode) {
            return new PaintingData(canvasCode);
        }

        /**
         * @todo: [HIGH] Use placeholders
         * @param resolution
         * @param width
         * @param height
         * @return
         */
        @Override
        public PaintingData createFresh(String canvasCode, Resolution resolution, int width, int height) {
            final PaintingData newPainting = new PaintingData(canvasCode);

            byte[] color = new byte[width * height * 4];
            ByteBuffer defaultColorBuffer = ByteBuffer.wrap(color);

            for (int x = 0; x < width * height; x++) {
                defaultColorBuffer.putInt(x * 4, Helper.CANVAS_COLOR);
            }

            newPainting.wrapData(resolution, width, height, color);

            return newPainting;
        }

        @Override
        public PaintingData createWrap(String canvasCode, Resolution resolution, int width, int height, byte[] color) {
            final PaintingData newPainting = new PaintingData(canvasCode);
            newPainting.wrapData(resolution, width, height, color);

            return newPainting;
        }

        /*
         * Networking
         */

        @Override
        public PaintingData readPacketData(PacketBuffer networkBuffer) {
            final String canvasCode = networkBuffer.readUtf(Helper.CANVAS_CODE_MAX_LENGTH);

            final PaintingData newPainting = new PaintingData(canvasCode);

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

            final UUID authorUuid = networkBuffer.readUUID();
            final String authorName = networkBuffer.readUtf(64);
            final String name = networkBuffer.readUtf(32);

            newPainting.setMetaProperties(
                authorUuid,
                authorName,
                name
            );

            return newPainting;
        }

        public void writePacketData(String canvasCode, PaintingData canvasData, PacketBuffer networkBuffer) {
            networkBuffer.writeUtf(canvasCode, Helper.CANVAS_CODE_MAX_LENGTH);
            networkBuffer.writeByte(canvasData.resolution.ordinal());
            networkBuffer.writeInt(canvasData.width);
            networkBuffer.writeInt(canvasData.height);
            networkBuffer.writeInt(canvasData.getColorDataBuffer().remaining());
            networkBuffer.writeBytes(canvasData.getColorDataBuffer());

            // @todo: [LOW] Compatibility code, remove on release
            if (canvasData.authorUuid != null) {
                networkBuffer.writeUUID(canvasData.authorUuid);
            } else {
                networkBuffer.writeUUID(FALLBACK_UUID);
            }

            networkBuffer.writeUtf(canvasData.authorName, 64);
            networkBuffer.writeUtf(canvasData.name, 32);
        }
    }
}

