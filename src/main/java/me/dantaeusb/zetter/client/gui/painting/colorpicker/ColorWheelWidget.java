package me.dantaeusb.zetter.client.gui.painting.colorpicker;

import me.dantaeusb.zetter.client.gui.PaintingScreen;
import me.dantaeusb.zetter.client.gui.painting.AbstractPaintingWidget;
import me.dantaeusb.zetter.client.gui.painting.base.SliderWidget;
import me.dantaeusb.zetter.client.gui.painting.util.ZetterColorPickerRenderer;
import me.dantaeusb.zetter.core.ZetterRenderTypes;
import me.dantaeusb.zetter.core.tools.Color;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.network.chat.Component;

public class ColorWheelWidget extends AbstractPaintingWidget implements Renderable {
  private final PaletteWidget paletteWidget;
  private final SliderWidget wheelLightnessSlider;

  public ColorWheelWidget(PaintingScreen parentScreen, int x, int y) {
    super(parentScreen, x, y, 0, 0, Component.translatable("screen.zetter.painting.color_picker.color_wheel"));

    final int PALETTE_WIDGET_POSITION_X = 5;
    final int PALETTE_WIDGET_POSITION_Y = 3;

    final int WHEEL_LIGHTNESS_SLIDER_POSITION_X = 150;
    final int WHEEL_LIGHTNESS_SLIDER_POSITION_Y = 3;

    this.paletteWidget = new PaletteWidget(
        parentScreen,
        x + PALETTE_WIDGET_POSITION_X,
        y + PALETTE_WIDGET_POSITION_Y,
        PaletteWidget.Orientation.VERTICAL
    );

    this.wheelLightnessSlider = new SliderWidget(
        parentScreen,
        x + WHEEL_LIGHTNESS_SLIDER_POSITION_X,
        y + WHEEL_LIGHTNESS_SLIDER_POSITION_Y,
        Component.translatable("screen.zetter.painting.color_picker.lightness"),
        this::updateLightness,
        SliderWidget.Orientation.VERTICAL,
        this::renderLightnessVerticalSliderBackground,
        null
    );
  }

  private void updateLightness(float value) {
    Color oldColor = this.parentScreen.getPaintingScreenState().currentColor();
    Color newColor;
    if (this.parentScreen.getPaintingScreenState().colorSpace() == PaintingScreen.ColorSpace.okHSL) {
      newColor = new Color(oldColor.getOkHsl().x, oldColor.getOkHsl().y, value);
    } else {
      newColor = new Color(oldColor.getHsl().x, oldColor.getHsl().y, value);
    }

    this.parentScreen.setPaintingScreenState(this.parentScreen.getPaintingScreenState().withCurrentColor(newColor));
  }

  @Override
  public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    final int COLOR_WHEEL_U = 0;
    final int COLOR_WHEEL_V = 0;
    final int COLOR_WHEEL_POSITION_X = 29;
    final int COLOR_WHEEL_POSITION_Y = 1;
    final int COLOR_WHEEL_WIDTH = 120;
    final int COLOR_WHEEL_HEIGHT = 120;

    if (this.parentScreen.getPaintingScreenState().colorSpace() == PaintingScreen.ColorSpace.okHSL) {
      ZetterColorPickerRenderer.renderColorPicker(
          guiGraphics,
          ZetterRenderTypes.RenderMode.OK_HUE_SATURATION,
          this.parentScreen.getPaintingScreenState().currentColor().getOkHsl(),
          this.getX() + COLOR_WHEEL_POSITION_X + 2,
          this.getY() + COLOR_WHEEL_POSITION_Y + 2,
          COLOR_WHEEL_WIDTH - 4,
          COLOR_WHEEL_HEIGHT - 4
      );
    } else {
      ZetterColorPickerRenderer.renderColorPicker(
          guiGraphics,
          ZetterRenderTypes.RenderMode.RGB_HUE_SATURATION,
          this.parentScreen.getPaintingScreenState().currentColor().getHsl(),
          this.getX() + COLOR_WHEEL_POSITION_X + 2,
          this.getY() + COLOR_WHEEL_POSITION_Y + 2,
          COLOR_WHEEL_WIDTH - 4,
          COLOR_WHEEL_HEIGHT - 4
      );
    }

    guiGraphics.blit(PAINTING_WIDGETS_TEXTURE_RESOURCE, this.getX() + COLOR_WHEEL_POSITION_X, this.getY() + COLOR_WHEEL_POSITION_Y, COLOR_WHEEL_U, COLOR_WHEEL_V, COLOR_WHEEL_WIDTH, COLOR_WHEEL_HEIGHT);
    this.renderCurrentColor(guiGraphics, mouseX, mouseY, partialTick);

    this.paletteWidget.render(guiGraphics, mouseX, mouseY, partialTick);
    this.wheelLightnessSlider.render(guiGraphics, mouseX, mouseY, partialTick);
  }

  protected void renderCurrentColor(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    final int CURRENT_COLOR_U = 90;
    final int CURRENT_COLOR_V = 189;
    final int CURRENT_COLOR_WIDTH = 10;
    final int CURRENT_COLOR_HEIGHT = 10;

    guiGraphics.blit(PAINTING_WIDGETS_TEXTURE_RESOURCE, this.getX() + 150, this.getY() + 16, CURRENT_COLOR_U, CURRENT_COLOR_V, CURRENT_COLOR_WIDTH, CURRENT_COLOR_HEIGHT);
  }

  protected void renderLightnessVerticalSliderBackground(GuiGraphics guiGraphics, int x, int y, int width, int height, float value) {
    if (this.parentScreen.getPaintingScreenState().colorSpace() == PaintingScreen.ColorSpace.okHSL) {
      ZetterColorPickerRenderer.renderColorPicker(
          guiGraphics,
          ZetterRenderTypes.RenderMode.OK_LIGHTNESS_VERTICAL,
          this.parentScreen.getPaintingScreenState().currentColor().getOkHsl(),
          x,
          y,
          width,
          height
      );
    } else {
      ZetterColorPickerRenderer.renderColorPicker(
          guiGraphics,
          ZetterRenderTypes.RenderMode.RGB_LIGHTNESS_VERTICAL,
          this.parentScreen.getPaintingScreenState().currentColor().getHsl(),
          x,
          y,
          width,
          height
      );
    }

  }
}
