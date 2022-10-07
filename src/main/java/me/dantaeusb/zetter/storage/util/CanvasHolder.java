package me.dantaeusb.zetter.storage.util;

import me.dantaeusb.zetter.storage.AbstractCanvasData;

public class CanvasHolder <T extends AbstractCanvasData> {
    public final String code;
    public final T data;

    public CanvasHolder(String canvasName, T canvasData) {
        this.code = canvasName;
        this.data = canvasData;
    }
}
