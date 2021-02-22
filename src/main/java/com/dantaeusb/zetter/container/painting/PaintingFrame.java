package com.dantaeusb.zetter.container.painting;

import java.util.UUID;

public class PaintingFrame {
    private long frameTime;
    private int pixelIndex;
    private int color;
    private UUID ownerId;

    public PaintingFrame(byte[] frameData) {

    }

    public PaintingFrame(long frameTime, short pixelIndex, int color, UUID ownerId) {
        this.frameTime = frameTime;
        this.pixelIndex = (int) pixelIndex;
        this.color = color;
        this.ownerId = ownerId;
    }

    public long getFrameTime() {
        return frameTime;
    }

    public int getPixelIndex() {
        return pixelIndex;
    }

    public int getColor() {
        return color;
    }

    @Override
    public String toString(){
        return String.format("Pixel: %d with color %X changed by %s", this.pixelIndex, this.color, this.ownerId.toString());
    }
}
