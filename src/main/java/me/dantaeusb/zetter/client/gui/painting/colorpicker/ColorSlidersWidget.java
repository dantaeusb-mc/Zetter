package me.dantaeusb.zetter.client.gui.painting.colorpicker;

import me.dantaeusb.zetter.client.gui.PaintingScreen;
import me.dantaeusb.zetter.client.gui.painting.AbstractPaintingWidget;
import me.dantaeusb.zetter.client.gui.painting.base.SliderWidget;
import me.dantaeusb.zetter.core.tools.Color;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.network.chat.Component;
import org.joml.Vector3f;

public class ColorSlidersWidget extends AbstractPaintingWidget implements Renderable {
  private final SliderWidget hueSlider;
  private final SliderWidget saturationSlider;
  private final SliderWidget lightnessSlider;

  public ColorSlidersWidget(PaintingScreen parentScreen, int x, int y) {
    super(parentScreen, x, y, 164, 120, Component.translatable("screen.zetter.painting.color_picker.sliders"));

    this.hueSlider = new SliderWidget(
        parentScreen,
        x,
        y,
        Component.translatable("screen.zetter.painting.color_picker.hue"),
        this::updateHue,
        SliderWidget.Orientation.HORIZONTAL,
        this::renderHueSliderBackground,
        null
    );

    this.saturationSlider = new SliderWidget(
        parentScreen,
        x,
        y,
        Component.translatable("screen.zetter.painting.color_picker.saturation"),
        this::updateSaturation,
        SliderWidget.Orientation.HORIZONTAL,
        this::renderSaturationSliderBackground,
        null
    );

    this.lightnessSlider = new SliderWidget(
        parentScreen,
        x,
        y,
        Component.translatable("screen.zetter.painting.color_picker.lightness"),
        this::updateLightness,
        SliderWidget.Orientation.HORIZONTAL,
        this::renderLightnessSliderBackground,
        null
    );
  }

  private void updateHue(float value) {
    Color oldColor = this.parentScreen.getPaintingScreenState().currentColor();
    Color newColor;
    if (this.parentScreen.getPaintingScreenState().colorSpace() == PaintingScreen.ColorSpace.okHSL) {
      newColor = new Color(value, oldColor.getOkHsl().y, oldColor.getOkHsl().z);
    } else {
      newColor = new Color(value, oldColor.getHsl().y, oldColor.getHsl().z);
    }

    this.parentScreen.setPaintingScreenState(this.parentScreen.getPaintingScreenState().withCurrentColor(newColor));
  }

  private void updateSaturation(float value) {
    Color oldColor = this.parentScreen.getPaintingScreenState().currentColor();
    Color newColor;
    if (this.parentScreen.getPaintingScreenState().colorSpace() == PaintingScreen.ColorSpace.okHSL) {
      newColor = new Color(oldColor.getOkHsl().x, value, oldColor.getOkHsl().z);
    } else {
      newColor = new Color(oldColor.getHsl().x, value, oldColor.getHsl().z);
    }

    this.parentScreen.setPaintingScreenState(this.parentScreen.getPaintingScreenState().withCurrentColor(newColor));
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
    this.hueSlider.render(guiGraphics, mouseX, mouseY, partialTick);
    this.saturationSlider.render(guiGraphics, mouseX, mouseY, partialTick);
    this.lightnessSlider.render(guiGraphics, mouseX, mouseY, partialTick);
  }

  public void renderHueSliderBackground(GuiGraphics guiGraphics, int x, int y, int width, int height, float value) {
    guiGraphics.fillGradient(x, y, x + width, y + height, 0xFF000000, 0xFFFFFFFF);
  }

  public void renderSaturationSliderBackground(GuiGraphics guiGraphics, int x, int y, int width, int height, float value) {
    guiGraphics.fillGradient(x, y, x + width, y + height, 0xFF000000, 0xFFFFFFFF);
  }

  public void renderLightnessSliderBackground(GuiGraphics guiGraphics, int x, int y, int width, int height, float value) {
    guiGraphics.fillGradient(x, y, x + width, y + height, 0xFF000000, 0xFFFFFFFF);
  }
}
