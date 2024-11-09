package me.dantaeusb.zetter.client.gui.painting;

import me.dantaeusb.zetter.client.gui.EaselScreen;
import me.dantaeusb.zetter.client.gui.PaintingScreen;
import me.dantaeusb.zetter.client.gui.painting.base.TabsWidget;
import me.dantaeusb.zetter.client.gui.painting.colorpicker.ColorSlidersWidget;
import me.dantaeusb.zetter.client.gui.painting.colorpicker.ColorWheelWidget;
import me.dantaeusb.zetter.client.gui.painting.tool.AbstractTabGroupWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.network.chat.Component;

import java.util.HashMap;

public class ColorPickerWidget extends AbstractTabGroupWidget implements Renderable {
  private final static int WIDTH = 164;
  private final static int HEIGHT = 136;

  private final int SPACE_TOGGLE_X = 123;
  private final int SPACE_TOGGLE_WIDTH = 36;

  public ColorPickerWidget(PaintingScreen parentScreen, int x, int y) {
    super(
        parentScreen, x, y, WIDTH, HEIGHT,
        Component.translatable("screen.zetter.painting.color_picker"),
        new TabsWidget.Tab[]{
            new TabsWidget.Tab(Mode.WHEEL.code, Component.translatable("screen.zetter.painting.color_picker.wheel"), parentScreen.getFont()),
            new TabsWidget.Tab(Mode.SLIDERS.code, Component.translatable("screen.zetter.painting.color_picker.sliders"), parentScreen.getFont())
        },
        new HashMap<String, AbstractPaintingWidget>() {
          {
            put(Mode.WHEEL.code, new ColorWheelWidget(parentScreen, x, y + 13));
            put(Mode.SLIDERS.code, new ColorSlidersWidget(parentScreen, x, y + 13));
          }
        }
    );
  }

  @Override
  public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
    super.renderWidget(guiGraphics, mouseX, mouseY, partialTicks);

    final int TAB_U = 90;
    final int TAB_V = 151;

    final int PADDING_H = 4;
    final int PADDING_V = 3;

    final int SLICE = 2;

    Component ok = Component.literal("ok");
    Component hsl = Component.literal("HSL");

    guiGraphics.blitNineSliced(PAINTING_WIDGETS_TEXTURE_RESOURCE, this.getX() + SPACE_TOGGLE_X, this.getY(), SPACE_TOGGLE_WIDTH, 13, SLICE, SLICE, 5, 13, TAB_U + 6, TAB_V);

    if (this.parentScreen.getPaintingScreenState().colorSpace().equals(PaintingScreen.ColorSpace.okHSL)) {
      guiGraphics.drawString(this.parentScreen.getFont(), ok, this.getX() + SPACE_TOGGLE_X + PADDING_H, this.getY() + PADDING_V, 0xFF40FF40, false);
    } else {
      guiGraphics.drawString(this.parentScreen.getFont(), ok, this.getX() + SPACE_TOGGLE_X + PADDING_H, this.getY() + PADDING_V, 0xFFFF4040, false);
    }

    guiGraphics.drawString(this.parentScreen.getFont(), hsl, this.getX() + SPACE_TOGGLE_X + PADDING_H + this.parentScreen.getFont().width(ok), this.getY() + PADDING_V, 0xFF404040, false);
  }

  @Override
  public boolean mouseClicked(double mouseX, double mouseY, int button) {
    int iMouseX = (int) mouseX;
    int iMouseY = (int) mouseY;

    // Quick check
    if (!this.isMouseOver(mouseX, mouseY)) {
      return false;
    }

    if (isInRect(this.getX() + SPACE_TOGGLE_X, this.getY(), SPACE_TOGGLE_WIDTH, 13, iMouseX, iMouseY)) {
      this.parentScreen.setPaintingScreenState(this.parentScreen.getPaintingScreenState().withColorSpace(
          this.parentScreen.getPaintingScreenState().colorSpace().equals(PaintingScreen.ColorSpace.okHSL) ? PaintingScreen.ColorSpace.HSL : PaintingScreen.ColorSpace.okHSL
      ));

      return true;
    }

    return super.mouseClicked(mouseX, mouseY, button);
  }

  public enum Mode {
    WHEEL("wheel"),
    SLIDERS("sliders");

    final String code;

    Mode(String code) {
      this.code = code;
    }
  }
}
