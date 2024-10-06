package me.dantaeusb.zetter.client.gui.painting.util.state;

import me.dantaeusb.zetter.client.gui.PaintingScreen;
import me.dantaeusb.zetter.core.tools.Color;
import me.dantaeusb.zetter.painting.Tools;

public record PaintingScreenState(
    Color currentColor,
    int currentPaletteSlot,
    Tools currentTool,
    PaintingScreen.CanvasMode canvasMode,
    PaintingScreen.ColorSpace colorSpace,
    CanvasOverlayState canvasOverlayState
) {
  public PaintingScreenState withCurrentColor(Color currentColor) {
    return new PaintingScreenState(currentColor, currentPaletteSlot, currentTool, canvasMode, colorSpace, canvasOverlayState);
  }

  public PaintingScreenState withCurrentPaletteSlot(int currentPaletteSlot) {
    return new PaintingScreenState(currentColor, currentPaletteSlot, currentTool, canvasMode, colorSpace, canvasOverlayState);
  }

  public PaintingScreenState withCurrentTool(Tools selectedTool) {
    return new PaintingScreenState(currentColor, currentPaletteSlot, selectedTool, canvasMode, colorSpace, canvasOverlayState);
  }

  public PaintingScreenState withCanvasMode(PaintingScreen.CanvasMode canvasMode) {
    return new PaintingScreenState(currentColor, currentPaletteSlot, currentTool, canvasMode, colorSpace, canvasOverlayState);
  }

  public PaintingScreenState withColorSpace(PaintingScreen.ColorSpace colorSpace) {
    return new PaintingScreenState(currentColor, currentPaletteSlot, currentTool, canvasMode, colorSpace, canvasOverlayState);
  }

  public PaintingScreenState withCanvasOverlayState(CanvasOverlayState canvasOverlayState) {
    return new PaintingScreenState(currentColor, currentPaletteSlot, currentTool, canvasMode, colorSpace, canvasOverlayState);
  }
}
