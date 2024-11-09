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
import org.joml.Vector3f;

public class ColorSlidersWidget extends AbstractPaintingWidget implements Renderable {
  private final SliderWidget hueSlider;
  private final SliderWidget saturationSlider;
  private final SliderWidget lightnessSlider;

  private final static int SLIDER_POSITION_X = 7;
  private final static int SLIDER_DISTANCE_GAP = 14;

  public ColorSlidersWidget(PaintingScreen parentScreen, int x, int y) {
    super(parentScreen, x, y, 164, 120, Component.translatable("screen.zetter.painting.color_picker.sliders"));

    this.hueSlider = new SliderWidget(
        parentScreen,
        x + SLIDER_POSITION_X,
        y + SLIDER_DISTANCE_GAP,
        Component.translatable("screen.zetter.painting.color_picker.hue"),
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
        Component.translatable("screen.zetter.painting.color_picker.saturation"),
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
        Component.translatable("screen.zetter.painting.color_picker.lightness"),
        this::getLightness,
        this::updateLightness,
        SliderWidget.Orientation.HORIZONTAL,
        this::renderLightnessSliderBackground,
        null
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
    if (this.parentScreen.getPaintingScreenState().colorSpace() == PaintingScreen.ColorSpace.okHSL) {
      return this.parentScreen.getPaintingScreenState().currentColor().getOkHsl().x;
    }

    return this.parentScreen.getPaintingScreenState().currentColor().getHsl().x;
  }

  private void updateHue(float value) {
    Color oldColor = this.parentScreen.getPaintingScreenState().currentColor();
    Color newColor;
    if (this.parentScreen.getPaintingScreenState().colorSpace() == PaintingScreen.ColorSpace.okHSL) {
      newColor = Color.fromOkHsl(new Vector3f(value, oldColor.getOkHsl().y, oldColor.getOkHsl().z));
    } else {
      newColor = Color.fromHsl(new Vector3f(value, oldColor.getHsl().y, oldColor.getHsl().z));
    }

    this.parentScreen.setPaintingScreenState(this.parentScreen.getPaintingScreenState().withCurrentColor(newColor));
  }

  private float getSaturation() {
    if (this.parentScreen.getPaintingScreenState().colorSpace() == PaintingScreen.ColorSpace.okHSL) {
      return this.parentScreen.getPaintingScreenState().currentColor().getOkHsl().y;
    }

    return this.parentScreen.getPaintingScreenState().currentColor().getHsl().y;
  }

  private void updateSaturation(float value) {
    Color oldColor = this.parentScreen.getPaintingScreenState().currentColor();
    Color newColor;
    if (this.parentScreen.getPaintingScreenState().colorSpace() == PaintingScreen.ColorSpace.okHSL) {
      newColor = Color.fromOkHsl(new Vector3f(oldColor.getOkHsl().x, value, oldColor.getOkHsl().z));
    } else {
      newColor = Color.fromHsl(new Vector3f(oldColor.getHsl().x, value, oldColor.getHsl().z));
    }

    this.parentScreen.setPaintingScreenState(this.parentScreen.getPaintingScreenState().withCurrentColor(newColor));
  }

  private float getLightness() {
    if (this.parentScreen.getPaintingScreenState().colorSpace() == PaintingScreen.ColorSpace.okHSL) {
      return this.parentScreen.getPaintingScreenState().currentColor().getOkHsl().z;
    }

    return this.parentScreen.getPaintingScreenState().currentColor().getHsl().z;
  }

  private void updateLightness(float value) {
    Color oldColor = this.parentScreen.getPaintingScreenState().currentColor();
    Color newColor;
    if (this.parentScreen.getPaintingScreenState().colorSpace() == PaintingScreen.ColorSpace.okHSL) {
      newColor = Color.fromOkHsl(new Vector3f(oldColor.getOkHsl().x, oldColor.getOkHsl().y, value));
    } else {
      newColor = Color.fromHsl(new Vector3f(oldColor.getHsl().x, oldColor.getHsl().y, value));
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
    if (this.parentScreen.getPaintingScreenState().colorSpace() == PaintingScreen.ColorSpace.okHSL) {
      ZetterColorPickerRenderer.renderColorPicker(
          guiGraphics,
          ZetterRenderTypes.RenderMode.OK_HUE_HORIZONTAL,
          this.parentScreen.getPaintingScreenState().currentColor().getOkHsl(),
          x,
          y,
          width,
          height
      );
    } else {
      ZetterColorPickerRenderer.renderColorPicker(
          guiGraphics,
          ZetterRenderTypes.RenderMode.RGB_HUE_HORIZONTAL,
          this.parentScreen.getPaintingScreenState().currentColor().getOkHsl(),
          x,
          y,
          width,
          height
      );
    }
  }

  public void renderSaturationSliderBackground(GuiGraphics guiGraphics, int x, int y, int width, int height, float value) {
    if (this.parentScreen.getPaintingScreenState().colorSpace() == PaintingScreen.ColorSpace.okHSL) {
      ZetterColorPickerRenderer.renderColorPicker(
          guiGraphics,
          ZetterRenderTypes.RenderMode.OK_SATURATION_HORIZONTAL,
          this.parentScreen.getPaintingScreenState().currentColor().getOkHsl(),
          x,
          y,
          width,
          height
      );
    } else {
      ZetterColorPickerRenderer.renderColorPicker(
          guiGraphics,
          ZetterRenderTypes.RenderMode.RGB_SATURATION_HORIZONTAL,
          this.parentScreen.getPaintingScreenState().currentColor().getOkHsl(),
          x,
          y,
          width,
          height
      );
    }
  }

  public void renderLightnessSliderBackground(GuiGraphics guiGraphics, int x, int y, int width, int height, float value) {
    if (this.parentScreen.getPaintingScreenState().colorSpace() == PaintingScreen.ColorSpace.okHSL) {
      ZetterColorPickerRenderer.renderColorPicker(
          guiGraphics,
          ZetterRenderTypes.RenderMode.OK_LIGHTNESS_HORIZONTAL,
          this.parentScreen.getPaintingScreenState().currentColor().getOkHsl(),
          x,
          y,
          width,
          height
      );
    } else {
      ZetterColorPickerRenderer.renderColorPicker(
          guiGraphics,
          ZetterRenderTypes.RenderMode.RGB_LIGHTNESS_HORIZONTAL,
          this.parentScreen.getPaintingScreenState().currentColor().getOkHsl(),
          x,
          y,
          width,
          height
      );
    }
  }
}
