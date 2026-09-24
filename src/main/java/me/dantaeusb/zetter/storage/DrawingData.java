package me.dantaeusb.zetter.storage;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.core.ZetterCanvasTypes;

/**
 * A drawing belongs to the entity it was made on rather than to an item.
 *
 * It comes into being the first time somebody marks the surface, not when that
 * surface is placed — a world full of decorative boards nobody has drawn on should
 * cost nothing — it lives as long as the entity does, across logouts and restarts,
 * and it goes when the entity is destroyed.
 *
 * That lifetime is what decides how it is named. A canvas is handed an id from a
 * pool because it travels between items and has to be findable from any of them; a
 * drawing never leaves the entity, so it takes the entity's own UUID and needs no
 * pool, no allocation and no freeing. The runtime entity id would not do: it is
 * assigned afresh every time the entity loads.
 *
 * @see CanvasData
 */
public class DrawingData extends CanvasData {
    public static final String TYPE = "drawing";

    public static final String CODE_PREFIX = Zetter.MOD_ID + "_" + TYPE + "_";

    /**
     * Its own, since the default is what stands in before a drawing exists and it
     * has to be empty where a canvas' is primed
     */
    private static final String DEFAULT_CODE_PREFIX = CODE_PREFIX + DEFAULT_CODE_MARKER;

    /**
     * Named after the entity carrying it, which is the one thing about it that is
     * the same before and after a restart
     *
     * @param holderId
     * @return
     */
    public static String getCanvasCode(java.util.UUID holderId) {
        return CODE_PREFIX + holderId;
    }

    /**
     * @param canvasCode
     * @return
     */
    public static boolean isDrawingCode(String canvasCode) {
        return canvasCode.startsWith(CODE_PREFIX);
    }

    public static final CanvasDataBuilder<DrawingData> BUILDER = new DrawingCanvasDataBuilder();

    /**
     * Nothing has been drawn here yet. Client side only, as with any default.
     *
     * @param widthBlocks
     * @param heightBlocks
     * @return
     */
    public static String getDefaultCanvasCode(int widthBlocks, int heightBlocks) {
        return DEFAULT_CODE_PREFIX + widthBlocks + "x" + heightBlocks;
    }

    protected DrawingData() {}

    @Override
    public CanvasDataType<? extends CanvasData> getType() {
        return ZetterCanvasTypes.DRAWING.get();
    }

    private static class DrawingCanvasDataBuilder extends CanvasData.AbstractCanvasDataBuilder<DrawingData> {
        @Override
        protected DrawingData create() {
            return new DrawingData();
        }
    }
}
