package me.dantaeusb.zetter.client.gui.painting.colorpicker;

import me.dantaeusb.zetter.client.gui.PaintingScreen;
import me.dantaeusb.zetter.client.gui.painting.AbstractPaintingGroupWidget;
import me.dantaeusb.zetter.client.gui.painting.base.SliderWidget;
import me.dantaeusb.zetter.client.gui.painting.util.ZetterColorPickerRenderer;
import me.dantaeusb.zetter.core.ZetterRenderTypes;
import me.dantaeusb.zetter.core.tools.Color;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

public class ColorSlidersWidget extends AbstractPaintingGroupWidget implements Renderable {
  private final SliderWidget hueSlider;
  private final SliderWidget saturationSlider;
  private final SliderWidget lightnessSlider;

  private final static int SLIDER_POSITION_X = 5;
  private final static int SLIDER_DISTANCE_GAP = 14;

  private final ColorPaletteWidget colorPaletteWidget;
  private final ColorCodeWidget colorCodeWidget;
  private final ColorPreviewWidget colorPreviewWidget;

  public ColorSlidersWidget(PaintingScreen parentScreen, int x, int y) {
    super(parentScreen, x, y, 164, 120, Component.translatable("screen.zetter.painting.color_picker.sliders"));

    final int COLOR_PREVIEW_WIDGET_POSITION_X = 5;
    final int COLOR_PREVIEW_WIDGET_POSITION_Y = 95;

    final int PALETTE_WIDGET_POSITION_X = 5 + ColorPreviewWidget.COLOR_PREVIEW_TOOL_WIDTH + 4;
    final int PALETTE_WIDGET_POSITION_Y = 98;

    this.colorPreviewWidget = new ColorPreviewWidget(
        parentScreen,
        x + COLOR_PREVIEW_WIDGET_POSITION_X,
        y + COLOR_PREVIEW_WIDGET_POSITION_Y
    );
    this.addWidget(this.colorPreviewWidget);

    this.colorPaletteWidget = new ColorPaletteWidget(
        parentScreen,
        x + PALETTE_WIDGET_POSITION_X,
        y + PALETTE_WIDGET_POSITION_Y,
        ColorPaletteWidget.Orientation.HORIZONTAL
    );
    this.addWidget(this.colorPaletteWidget);

    this.hueSlider = new SliderWidget(
        parentScreen,
        x + SLIDER_POSITION_X,
        y + SLIDER_DISTANCE_GAP,
        Component.translatable("screen.zetter.painting.color_picker.sliders.hue"),
        this::getHue,
        this::updateHue,
        SliderWidget.Orientation.HORIZONTAL,
        this::renderHueSliderBackground,
        null
    );
    this.addWidget(this.hueSlider);

    this.saturationSlider = new SliderWidget(
        parentScreen,
        x + SLIDER_POSITION_X,
        y + SliderWidget.HORIZONTAL_HEIGHT + SLIDER_DISTANCE_GAP * 2,
        Component.translatable("screen.zetter.painting.color_picker.sliders.saturation"),
        this::getSaturation,
        this::updateSaturation,
        SliderWidget.Orientation.HORIZONTAL,
        this::renderSaturationSliderBackground,
        null
    );
    this.addWidget(this.saturationSlider);

    this.lightnessSlider = new SliderWidget(
        parentScreen,
        x + SLIDER_POSITION_X,
        y + SliderWidget.HORIZONTAL_HEIGHT * 2 + SLIDER_DISTANCE_GAP * 3,
        Component.translatable("screen.zetter.painting.color_picker.sliders.lightness"),
        this::getLightness,
        this::updateLightness,
        SliderWidget.Orientation.HORIZONTAL,
        this::renderLightnessSliderBackground,
        null
    );
    this.addWidget(this.lightnessSlider);

    this.colorCodeWidget = new ColorCodeWidget(
        parentScreen,
        x + 164 - ColorCodeWidget.TEXTBOX_WIDTH - SLIDER_POSITION_X,
        y + SliderWidget.HORIZONTAL_HEIGHT * 3 + SLIDER_DISTANCE_GAP * 3 + 4
    );
    this.addWidget(this.colorCodeWidget);
  }

  private float getHue() {
    if (this.parentScreen.getEaselState().colorSpace() == PaintingScreen.ColorSpace.okHSL) {
      return this.parentScreen.getCurrentColor().getOkHsl().x;
    }

    return this.parentScreen.getCurrentColor().getHsl().x;
  }

  private void updateHue(float value) {
    Color oldColor = this.parentScreen.getCurrentColor();
    Color newColor;
    if (this.parentScreen.getEaselState().colorSpace() == PaintingScreen.ColorSpace.okHSL) {
      newColor = Color.fromOkHsl(new Vector3f(value, oldColor.getOkHsl().y, oldColor.getOkHsl().z));
    } else {
      newColor = Color.fromHsl(new Vector3f(value, oldColor.getHsl().y, oldColor.getHsl().z));
    }

    this.parentScreen.setCurrentColor(newColor);
  }

  private float getSaturation() {
    if (this.parentScreen.getEaselState().colorSpace() == PaintingScreen.ColorSpace.okHSL) {
      return this.parentScreen.getCurrentColor().getOkHsl().y;
    }

    return this.parentScreen.getCurrentColor().getHsl().y;
  }

  private void updateSaturation(float value) {
    Color oldColor = this.parentScreen.getCurrentColor();
    Color newColor;
    if (this.parentScreen.getEaselState().colorSpace() == PaintingScreen.ColorSpace.okHSL) {
      newColor = Color.fromOkHsl(new Vector3f(oldColor.getOkHsl().x, value, oldColor.getOkHsl().z));
    } else {
      newColor = Color.fromHsl(new Vector3f(oldColor.getHsl().x, value, oldColor.getHsl().z));
    }

    this.parentScreen.setCurrentColor(newColor);
  }

  private float getLightness() {
    if (this.parentScreen.getEaselState().colorSpace() == PaintingScreen.ColorSpace.okHSL) {
      return this.parentScreen.getCurrentColor().getOkHsl().z;
    }

    return this.parentScreen.getCurrentColor().getHsl().z;
  }

  private void updateLightness(float value) {
    Color oldColor = this.parentScreen.getCurrentColor();
    Color newColor;
    if (this.parentScreen.getEaselState().colorSpace() == PaintingScreen.ColorSpace.okHSL) {
      newColor = Color.fromOkHsl(new Vector3f(oldColor.getOkHsl().x, oldColor.getOkHsl().y, value));
    } else {
      newColor = Color.fromHsl(new Vector3f(oldColor.getHsl().x, oldColor.getHsl().y, value));
    }

    this.parentScreen.setCurrentColor(newColor);
  }

  @Override
  public void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    guiGraphics.drawString(
        this.parentScreen.getFont(),
        this.hueSlider.getMessage().getString(),
        this.getX() + SLIDER_POSITION_X,
        this.getY() + 4,
        Color.DARK_GRAY.getARGB(), false
    );

    this.hueSlider.render(guiGraphics, mouseX, mouseY, partialTick);

    guiGraphics.drawString(
        this.parentScreen.getFont(),
        this.saturationSlider.getMessage().getString(),
        this.getX() + SLIDER_POSITION_X,
        this.getY() + SliderWidget.HORIZONTAL_HEIGHT + SLIDER_DISTANCE_GAP + 4,
        Color.DARK_GRAY.getARGB(), false
    );

    this.saturationSlider.render(guiGraphics, mouseX, mouseY, partialTick);

    guiGraphics.drawString(
        this.parentScreen.getFont(),
        this.lightnessSlider.getMessage().getString(),
        this.getX() + SLIDER_POSITION_X,
        this.getY() + SliderWidget.HORIZONTAL_HEIGHT * 2 + SLIDER_DISTANCE_GAP * 2 + 4,
        Color.DARK_GRAY.getARGB(), false
    );

    this.lightnessSlider.render(guiGraphics, mouseX, mouseY, partialTick);

    guiGraphics.drawString(
        this.parentScreen.getFont(),
        this.colorCodeWidget.getMessage(),
        this.getX() + SLIDER_POSITION_X,
        this.getY() + SliderWidget.HORIZONTAL_HEIGHT * 3 + SLIDER_DISTANCE_GAP * 3 + 8,
        Color.DARK_GRAY.getARGB(), false
    );

    this.colorCodeWidget.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
    this.colorPaletteWidget.render(guiGraphics, mouseX, mouseY, partialTick);
    this.colorPreviewWidget.render(guiGraphics, mouseX, mouseY, partialTick);
  }

  public void renderHueSliderBackground(GuiGraphics guiGraphics, int x, int y, int width, int height, float value) {
    if (this.parentScreen.getEaselState().colorSpace() == PaintingScreen.ColorSpace.okHSL) {
      ZetterColorPickerRenderer.renderColorPicker(
          guiGraphics,
          ZetterRenderTypes.RenderMode.OK_HUE_HORIZONTAL,
          this.parentScreen.getCurrentColor().getOkHsl(),
          x,
          y,
          width,
          height
      );
    } else {
      ZetterColorPickerRenderer.renderColorPicker(
          guiGraphics,
          ZetterRenderTypes.RenderMode.RGB_HUE_HORIZONTAL,
          this.parentScreen.getCurrentColor().getOkHsl(),
          x,
          y,
          width,
          height
      );
    }
  }

  public void renderSaturationSliderBackground(GuiGraphics guiGraphics, int x, int y, int width, int height, float value) {
    if (this.parentScreen.getEaselState().colorSpace() == PaintingScreen.ColorSpace.okHSL) {
      ZetterColorPickerRenderer.renderColorPicker(
          guiGraphics,
          ZetterRenderTypes.RenderMode.OK_SATURATION_HORIZONTAL,
          this.parentScreen.getCurrentColor().getOkHsl(),
          x,
          y,
          width,
          height
      );
    } else {
      ZetterColorPickerRenderer.renderColorPicker(
          guiGraphics,
          ZetterRenderTypes.RenderMode.RGB_SATURATION_HORIZONTAL,
          this.parentScreen.getCurrentColor().getOkHsl(),
          x,
          y,
          width,
          height
      );
    }
  }

  public void renderLightnessSliderBackground(GuiGraphics guiGraphics, int x, int y, int width, int height, float value) {
    if (this.parentScreen.getEaselState().colorSpace() == PaintingScreen.ColorSpace.okHSL) {
      ZetterColorPickerRenderer.renderColorPicker(
          guiGraphics,
          ZetterRenderTypes.RenderMode.OK_LIGHTNESS_HORIZONTAL,
          this.parentScreen.getCurrentColor().getOkHsl(),
          x,
          y,
          width,
          height
      );
    } else {
      ZetterColorPickerRenderer.renderColorPicker(
          guiGraphics,
          ZetterRenderTypes.RenderMode.RGB_LIGHTNESS_HORIZONTAL,
          this.parentScreen.getCurrentColor().getOkHsl(),
          x,
          y,
          width,
          height
      );
    }
  }
}
