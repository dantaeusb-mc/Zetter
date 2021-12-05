package com.dantaeusb.zetter.storage;

import com.dantaeusb.zetter.Zetter;
import net.minecraft.nbt.CompoundNBT;

/**
 * It's not enough to just init data, we need to register it with
 * @see com.dantaeusb.zetter.canvastracker.CanvasServerTracker::registerCanvasData();
 */
public class PaintingData extends AbstractCanvasData {
    public static final String CODE_PREFIX = Zetter.MOD_ID + "_painting_";

    protected static final String NBT_TAG_AUTHOR_NAME = "author_name";
    protected static final String NBT_TAG_TITLE = "title";

    protected String authorName;
    protected String title;

    public PaintingData(String canvasCode) {
        super(canvasCode);
    }

    public PaintingData(int paintingId) {
        super(getPaintingCode(paintingId));
    }

    public void copyFrom(AbstractCanvasData templateCanvasData) {
        if (this.isEditable()) {
            Zetter.LOG.error("Cannot copy to sealed canvas");
            return;
        }

        this.resolution = templateCanvasData.getResolution();
        this.width = templateCanvasData.getWidth();
        this.height = templateCanvasData.getHeight();
        this.updateColorData(templateCanvasData.color);
        this.setDirty();
    }

    public void setMetaProperties(String authorName, String title) {
        this.authorName = authorName;
        this.title = title;
    }

    public String getPaintingName() {
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

    public void load(CompoundNBT compound) {
        super.load(compound);

        this.authorName = compound.getString(NBT_TAG_AUTHOR_NAME);
        this.title = compound.getString(NBT_TAG_TITLE);
    }

    public CompoundNBT save(CompoundNBT compound) {
        super.save(compound);

        compound.putString(NBT_TAG_AUTHOR_NAME, this.authorName);
        compound.putString(NBT_TAG_TITLE, this.title);

        return compound;
    }
}

