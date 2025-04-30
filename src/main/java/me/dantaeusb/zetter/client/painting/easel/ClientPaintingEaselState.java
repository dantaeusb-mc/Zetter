package me.dantaeusb.zetter.client.painting.easel;

import me.dantaeusb.zetter.client.gui.PaintingScreen;
import me.dantaeusb.zetter.client.gui.painting.util.state.CanvasOverlayState;
import me.dantaeusb.zetter.client.painting.palette.ClientPaintingPaletteState;

public record ClientPaintingEaselState(
    PaintingScreen.CanvasMode canvasMode,
    PaintingScreen.ColorSpace colorSpace,
    CanvasOverlayState canvasOverlayState
) {
    public ClientPaintingEaselState withCanvasMode(PaintingScreen.CanvasMode canvasMode) {
        return new ClientPaintingEaselState(canvasMode, colorSpace, canvasOverlayState);
    }

    public ClientPaintingEaselState withColorSpace(PaintingScreen.ColorSpace colorSpace) {
        return new ClientPaintingEaselState(canvasMode, colorSpace, canvasOverlayState);
    }

    public ClientPaintingEaselState withCanvasOverlayState(CanvasOverlayState canvasOverlayState) {
        return new ClientPaintingEaselState(canvasMode, colorSpace, canvasOverlayState);
    }

    public ClientPaintingEaselState decreaseCanvasScale() {
        if (canvasOverlayState.canvasScale() <= CanvasOverlayState.MIN_SCALE) {
            return this;
        }

        return new ClientPaintingEaselState(
            canvasMode,
            colorSpace,
            canvasOverlayState.withCanvasScale(canvasOverlayState.canvasScale() - 1)
        );
    }

    public ClientPaintingEaselState increaseCanvasScale() {
        if (canvasOverlayState.canvasScale() >= CanvasOverlayState.MAX_SCALE) {
            return this;
        }

        return new ClientPaintingEaselState(
            canvasMode,
            colorSpace,
            canvasOverlayState.withCanvasScale(canvasOverlayState.canvasScale() + 1)
        );
    }
}
