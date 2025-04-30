package me.dantaeusb.zetter.client.painting.palette;

import me.dantaeusb.zetter.client.gui.PaintingScreen;
import me.dantaeusb.zetter.client.gui.painting.util.state.ToolsParameters;
import me.dantaeusb.zetter.painting.Tool;

public record ClientPaintingPaletteState(
    int currentPaletteSlot,
    Tool currentTool,
    ToolsParameters toolParameters,
    PaintingScreen.ColorSpace colorSpace
) {
  public ClientPaintingPaletteState withCurrentPaletteSlot(int currentPaletteSlot) {
    return new ClientPaintingPaletteState(currentPaletteSlot, currentTool, toolParameters, colorSpace);
  }

  public ClientPaintingPaletteState withCurrentTool(Tool selectedTool) {
    return new ClientPaintingPaletteState(currentPaletteSlot, selectedTool, toolParameters, colorSpace);
  }

  public ClientPaintingPaletteState withToolParameters(ToolsParameters toolParameters) {
    return new ClientPaintingPaletteState(currentPaletteSlot, currentTool, toolParameters, colorSpace);
  }

  public ClientPaintingPaletteState withColorSpace(PaintingScreen.ColorSpace colorSpace) {
    return new ClientPaintingPaletteState(currentPaletteSlot, currentTool, toolParameters, colorSpace);
  }
}
