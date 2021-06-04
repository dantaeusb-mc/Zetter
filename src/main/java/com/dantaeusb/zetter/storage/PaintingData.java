package com.dantaeusb.zetter.storage;

import com.dantaeusb.zetter.Zetter;

/**
 * It's not enough to just init data, we need to register it with
 * @see com.dantaeusb.zetter.canvastracker.CanvasServerTracker::registerCanvasData();
 */
public class PaintingData extends AbstractCanvasData {
    public static final String CODE_PREFIX = Zetter.MOD_ID + "_painting_";

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

        this.width = templateCanvasData.getWidth();
        this.height = templateCanvasData.getHeight();
        this.updateColorData(templateCanvasData.color);
        this.markDirty();
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
}

