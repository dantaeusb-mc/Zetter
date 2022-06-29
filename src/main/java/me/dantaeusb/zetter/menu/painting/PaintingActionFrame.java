package me.dantaeusb.zetter.menu.painting;

import java.util.UUID;

public class PaintingActionFrame {
    private long frameTime;
    private int pixelIndex;
    private int color;
    private UUID authorId;

    public PaintingActionFrame(byte[] frameData) {

    }

    public PaintingActionFrame(long frameTime, int pixelIndex, int color, UUID ownerId) {
        this.frameTime = frameTime;
        this.pixelIndex = pixelIndex;
        this.color = color;
        this.authorId = ownerId;
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
        return String.format("Pixel: %d with color %X changed by %s", this.pixelIndex, this.color, this.authorId.toString());
    }
}
