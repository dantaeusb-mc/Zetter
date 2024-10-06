package me.dantaeusb.zetter.client.gui.painting.util.state;

public record CanvasOverlayState(
    int canvasOffsetX,
    int canvasOffsetY,
    int canvasScale
) {
    public static final int MIN_SCALE = 1;
    public static final int MAX_SCALE = 3;

    public CanvasOverlayState withCanvasOffsetX(int canvasOffsetX) {
        return new CanvasOverlayState(canvasOffsetX, this.canvasOffsetY, this.canvasScale);
    }

    public CanvasOverlayState withCanvasOffsetY(int canvasOffsetY) {
        return new CanvasOverlayState(this.canvasOffsetX, canvasOffsetY, this.canvasScale);
    }

    public CanvasOverlayState withCanvasScale(int canvasScale) {
        return new CanvasOverlayState(this.canvasOffsetX, this.canvasOffsetY, canvasScale);
    }
}
