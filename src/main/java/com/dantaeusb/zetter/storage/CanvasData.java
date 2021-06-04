package com.dantaeusb.zetter.storage;

import com.dantaeusb.zetter.Zetter;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * It's not enough to just init data, we need to register it with
 * @see com.dantaeusb.zetter.canvastracker.CanvasServerTracker::registerCanvasData();
 */
public class CanvasData extends AbstractCanvasData {
    public static final String CODE_PREFIX = Zetter.MOD_ID + "_canvas_";

    public CanvasData(String canvasCode) {
        super(canvasCode);
    }

    public CanvasData(int canvasId) {
        super(getCanvasCode(canvasId));
    }

    public static String getCanvasCode(int canvasId) {
        return CODE_PREFIX + canvasId;
    }

    public void initData(int width, int height) {
        byte[] defaultColor = new byte[width * height * 4];
        ByteBuffer defaultColorBuffer = ByteBuffer.wrap(defaultColor);

        for (int x = 0; x < width * height; x++) {
            defaultColorBuffer.putInt(x * 4, 0xFFE0DACE);
        }

        this.initData(width, height, defaultColor);
    }

    public boolean isEditable() {
        return true;
    }

    public Type getType() {
        return Type.CANVAS;
    }
}

