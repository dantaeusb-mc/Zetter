package me.dantaeusb.zetter.client.gui.painting.util.state;

/**
 * Stores canvas transformations for overlay mode,
 * offset from the center of the screen and current scale.
 * @param canvasOffsetX Offset from the center
 * @param canvasOffsetY Offset from the center
 * @param canvasScale Scale of the canvas
 */
public record CanvasOverlayState(
    int canvasOffsetX,
    int canvasOffsetY,
    int canvasScale
) {
    public static final int MIN_SCALE = 1;
    public static final int MAX_SCALE = 6;

    public CanvasOverlayState withCanvasOffset(int canvasOffsetX, int canvasOffsetY) {
        return new CanvasOverlayState(canvasOffsetX, canvasOffsetY, this.canvasScale);
    }

    public CanvasOverlayState withCanvasScale(int canvasScale) {
        return new CanvasOverlayState(this.canvasOffsetX, this.canvasOffsetY, canvasScale);
    }
}
