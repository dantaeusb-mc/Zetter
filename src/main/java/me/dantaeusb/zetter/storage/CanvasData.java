package me.dantaeusb.zetter.storage;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.core.Helper;
import me.dantaeusb.zetter.canvastracker.CanvasServerTracker;
import net.minecraft.nbt.CompoundTag;

import java.nio.ByteBuffer;

/**
 * It's not enough to just init data, we need to register it with
 * @see CanvasServerTracker ::registerCanvasData();
 */
public class CanvasData extends AbstractCanvasData {
    public static final String CODE_PREFIX = Zetter.MOD_ID + "_canvas_";

    public static String getCanvasCode(int canvasId) {
        return CODE_PREFIX + canvasId;
    }

    protected CanvasData() {
        super();
    }

    /**
     * Create empty canvas data filled with canvas color
     * @param resolution
     * @param width
     * @param height
     * @return
     */
    public static CanvasData createFresh(Resolution resolution, int width, int height) {
        byte[] color = new byte[width * height * 4];
        ByteBuffer defaultColorBuffer = ByteBuffer.wrap(color);

        for (int x = 0; x < width * height; x++) {
            defaultColorBuffer.putInt(x * 4, Helper.CANVAS_COLOR);
        }

        final CanvasData newCanvas = new CanvasData();
        newCanvas.wrapData(resolution, width, height, color);

        return newCanvas;
    }

    /**
     * Create canvas from existing data
     * @param resolution
     * @param width
     * @param height
     * @param color
     * @return
     */
    public static CanvasData createWrap(Resolution resolution, int width, int height, byte[] color) {
        final CanvasData newCanvas = new CanvasData();
        newCanvas.wrapData(resolution, width, height, color);

        return newCanvas;
    }

    public static CanvasData createLoaded(CompoundTag compoundTag) {
        final CanvasData newCanvas = new CanvasData();
        newCanvas.load(compoundTag);

        return newCanvas;
    }

    public boolean isEditable() {
        return true;
    }

    public Type getType() {
        return Type.CANVAS;
    }
}

