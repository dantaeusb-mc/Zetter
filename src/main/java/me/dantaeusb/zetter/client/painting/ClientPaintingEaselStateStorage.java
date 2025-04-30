package me.dantaeusb.zetter.client.painting;

import me.dantaeusb.zetter.client.gui.PaintingScreen;
import me.dantaeusb.zetter.client.gui.painting.util.state.CanvasOverlayState;
import me.dantaeusb.zetter.client.gui.painting.util.state.ToolsParameters;
import me.dantaeusb.zetter.client.painting.easel.ClientPaintingEaselState;
import me.dantaeusb.zetter.client.painting.palette.ClientPaintingPaletteState;
import me.dantaeusb.zetter.painting.Tool;

import java.util.HashMap;
import java.util.UUID;

public class ClientPaintingEaselStateStorage {
  private static ClientPaintingEaselStateStorage instance;

  private final HashMap<UUID, ClientPaintingEaselState> paintingScreenStates = new HashMap<>();

  public ClientPaintingEaselState getPaintingPaletteState(UUID easelUuid) {
    if (this.paintingScreenStates.containsKey(easelUuid)) {
      return this.paintingScreenStates.get(easelUuid);
    }

    final ClientPaintingEaselState defaultState = new ClientPaintingEaselState(
        PaintingScreen.CanvasMode.IMMERSIVE_BACKGROUND,
        PaintingScreen.ColorSpace.okHSL,
        new CanvasOverlayState(
            0,
            0,
            1
        )
    );

    this.paintingScreenStates.put(easelUuid, defaultState);
    return defaultState;
  }

  public ClientPaintingEaselStateStorage() {
    instance = this;
  }

  public static ClientPaintingEaselStateStorage getInstance() {
    return instance;
  }
}
