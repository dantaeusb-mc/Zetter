package me.dantaeusb.zetter.storage;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.canvastracker.CanvasServerTracker;
import net.minecraft.nbt.CompoundTag;

/**
 * It's not enough to just init data, we need to register it with
 * @see CanvasServerTracker ::registerCanvasData();
 */
public class PaintingData extends AbstractCanvasData {
    public static final String CODE_PREFIX = Zetter.MOD_ID + "_painting_";

    protected static final String NBT_TAG_AUTHOR_NAME = "author_name";
    protected static final String NBT_TAG_TITLE = "title";

    public static final int MAX_GENERATION = 2;

    protected String authorName;
    protected String title;

    protected PaintingData() {
        super();
    }

    public static PaintingData createWrap(Resolution resolution, int width, int height, byte[] color) {
        final PaintingData newPainting = new PaintingData();
        newPainting.wrapData(resolution, width, height, color);

        return newPainting;
    }

    public static PaintingData createFrom(AbstractCanvasData templateCanvasData) {
        final PaintingData newPainting = new PaintingData();
        newPainting.wrapData(
                templateCanvasData.getResolution(),
                templateCanvasData.getWidth(),
                templateCanvasData.getHeight(),
                templateCanvasData.color
        );

        return newPainting;
    }

    public static PaintingData createLoaded(CompoundTag compoundTag) {
        final PaintingData newPainting = new PaintingData();
        newPainting.load(compoundTag);

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

    public boolean isEditable() {
        return false;
    }

    public Type getType() {
        return Type.PAINTING;
    }

    public void load(CompoundTag compoundTag) {
        super.load(compoundTag);

        this.authorName = compoundTag.getString(NBT_TAG_AUTHOR_NAME);
        this.title = compoundTag.getString(NBT_TAG_TITLE);
    }

    public CompoundTag save(CompoundTag compoundTag) {
        super.save(compoundTag);

        compoundTag.putString(NBT_TAG_AUTHOR_NAME, this.authorName);
        compoundTag.putString(NBT_TAG_TITLE, this.title);

        return compoundTag;
    }
}

