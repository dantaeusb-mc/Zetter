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
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

public class ColorSlidersWidget extends AbstractPaintingWidget implements Renderable {
  private final SliderWidget hueSlider;
  private final SliderWidget saturationSlider;
  private final SliderWidget lightnessSlider;

  private final static int SLIDER_POSITION_X = 5;
  private final static int SLIDER_DISTANCE_GAP = 14;

  private final PaletteWidget paletteWidget;
  private final ColorCodeWidget colorCodeWidget;

  public ColorSlidersWidget(PaintingScreen parentScreen, int x, int y) {
    super(parentScreen, x, y, 164, 120, Component.translatable("screen.zetter.painting.color_picker.sliders"));

    final int PALETTE_WIDGET_POSITION_X = 31;
    final int PALETTE_WIDGET_POSITION_Y = 95;

    this.paletteWidget = new PaletteWidget(
        parentScreen,
        x + PALETTE_WIDGET_POSITION_X,
        y + PALETTE_WIDGET_POSITION_Y,
        PaletteWidget.Orientation.HORIZONTAL
    );

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

    this.colorCodeWidget = new ColorCodeWidget(
        parentScreen,
        x + 164 - ColorCodeWidget.TEXTBOX_WIDTH - SLIDER_POSITION_X,
        y + SliderWidget.HORIZONTAL_HEIGHT * 3 + SLIDER_DISTANCE_GAP * 3 + 4
    );
  }

  @Override
  public boolean mouseClicked(double mouseX, double mouseY, int button) {
    if (!this.isMouseOver(mouseX, mouseY) || !this.isValidClickButton(button)) {
      return false;
    }

    if (this.hueSlider.mouseClicked(mouseX, mouseY, button)) {
      return true;
    }

    if (this.saturationSlider.mouseClicked(mouseX, mouseY, button)) {
      return true;
    }

    if (this.lightnessSlider.mouseClicked(mouseX, mouseY, button)) {
      return true;
    }

    return false;
  }

  @Override
  public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
    if (!this.active || !this.visible || !this.isValidClickButton(button)) {
      return false;
    }

    if (this.hueSlider.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) {
      return true;
    }

    if (this.saturationSlider.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) {
      return true;
    }

    if (this.lightnessSlider.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) {
      return true;
    }

    return false;
  }

  @Override
  public boolean mouseReleased(double mouseX, double mouseY, int button) {
    if (!this.active || !this.visible || !this.isValidClickButton(button)) {
      return false;
    }

    return this.hueSlider.mouseReleased(mouseX, mouseY, button)
        || this.saturationSlider.mouseReleased(mouseX, mouseY, button)
        || this.lightnessSlider.mouseReleased(mouseX, mouseY, button);
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
    this.paletteWidget.render(guiGraphics, mouseX, mouseY, partialTick);
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
