package me.dantaeusb.zetter.client.painting;

import me.dantaeusb.zetter.client.gui.PaintingScreen;
import me.dantaeusb.zetter.client.gui.painting.util.state.ToolsParameters;
import me.dantaeusb.zetter.client.painting.palette.ClientPaintingPaletteState;
import me.dantaeusb.zetter.painting.Tool;

import java.util.HashMap;
import java.util.UUID;

public class ClientPaintingPaletteStateStorage {
  private static ClientPaintingPaletteStateStorage instance;

  private final HashMap<UUID, ClientPaintingPaletteState> paintingScreenStates = new HashMap<>();

  public ClientPaintingPaletteState getPaintingPaletteState(UUID paletteUuid) {
    if (this.paintingScreenStates.containsKey(paletteUuid)) {
      return this.paintingScreenStates.get(paletteUuid);
    }

    final ClientPaintingPaletteState defaultState = new ClientPaintingPaletteState(
        0,
        Tool.PENCIL,
        new ToolsParameters(),
        PaintingScreen.ColorSpace.okHSL
    );

    this.paintingScreenStates.put(paletteUuid, defaultState);
    return defaultState;
  }

  public ClientPaintingPaletteStateStorage() {
    instance = this;
  }

  public static ClientPaintingPaletteStateStorage getInstance() {
    return instance;
  }
}
