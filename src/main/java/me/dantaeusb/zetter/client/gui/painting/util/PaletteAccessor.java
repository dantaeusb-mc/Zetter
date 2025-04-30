package me.dantaeusb.zetter.client.gui.painting.util;

import me.dantaeusb.zetter.core.tools.Color;
import me.dantaeusb.zetter.item.PaletteItem;
import net.minecraft.client.renderer.texture.Tickable;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;

/**
 * Accessor for palette item with extended color info cached
 * for different spaces
 */
public class PaletteAccessor {
  public static final int PALETTE_SLOTS = PaletteItem.PALETTE_SIZE;
  public static final long TICK_INTERVAL = 500L;

  private final ItemStack paletteStack;
  private final Color[] paletteColors;

  private boolean isDirty = false;
  private Long lastTick = null;

  public PaletteAccessor(ItemStack paletteStack) {
    this.paletteStack = paletteStack;
    this.paletteColors = new Color[PALETTE_SLOTS];

    final int[] paletteColors = PaletteItem.getPaletteColors(paletteStack);

    for (int i = 0; i < PALETTE_SLOTS; i++) {
      this.paletteColors[i] = new Color(paletteColors[i]);
    }
  }

  public Color[] getPaletteColors() {
    return paletteColors;
  }

  public Color getPaletteColor(int index) {
    if (index < 0 || index >= PALETTE_SLOTS) {
      throw new IndexOutOfBoundsException("Palette index out of bounds: " + index);
    }

    return this.paletteColors[index];
  }

  /**
   * @todo: [MED] add dirty & debounce
   * @param index
   * @param color
   */
  public void setPaletteColor(int index, Color color) {
    if (index < 0 || index >= PALETTE_SLOTS) {
      throw new IndexOutOfBoundsException("Palette index out of bounds: " + index);
    }

    this.paletteColors[index] = color;
    this.isDirty = true;
  }

  public void tick(long tick) {
    if (this.lastTick == null) {
      this.lastTick = tick;
    } else {
      if (tick - this.lastTick < TICK_INTERVAL) {
        this.lastTick = tick;
        return;
      }
    }

    if (this.isDirty) {
      this.writeColors();
    }
  }

  public void writeColors() {
    final int[] rawColors = Arrays
        .stream(this.paletteColors)
        .mapToInt(Color::getARGB)
        .toArray();

    PaletteItem.setPaletteColors(this.paletteStack, rawColors);
  }
}
