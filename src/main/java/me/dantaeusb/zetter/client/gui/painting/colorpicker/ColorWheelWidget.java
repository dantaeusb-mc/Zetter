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
import org.joml.Vector2f;
import org.joml.Vector3f;

public class ColorWheelWidget extends AbstractPaintingWidget implements Renderable {
  private final static int WIDTH = 164;
  private final static int HEIGHT = 135;

  private final int COLOR_WHEEL_POSITION_X = 29;
  private final int COLOR_WHEEL_POSITION_Y = 1;
  private final int COLOR_WHEEL_WIDTH = 120;
  private final int COLOR_WHEEL_HEIGHT = 120;

  private final PaletteWidget paletteWidget;
  private final SliderWidget wheelLightnessSlider;

  public ColorWheelWidget(PaintingScreen parentScreen, int x, int y) {
    super(parentScreen, x, y, WIDTH, HEIGHT, Component.translatable("screen.zetter.painting.color_picker.color_wheel"));

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
        this::getLightness,
        this::updateLightness,
        SliderWidget.Orientation.VERTICAL,
        this::renderLightnessVerticalSliderBackground,
        null
    );
  }

  private float getLightness() {
    if (this.parentScreen.getEaselState().colorSpace() == PaintingScreen.ColorSpace.okHSL) {
      return this.parentScreen.getCurrentColor().getOkHsl().z;
    } else {
      return this.parentScreen.getCurrentColor().getHsl().z;
    }
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
  public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    final int COLOR_WHEEL_U = 0;
    final int COLOR_WHEEL_V = 0;

    if (this.parentScreen.getEaselState().colorSpace() == PaintingScreen.ColorSpace.okHSL) {
      ZetterColorPickerRenderer.renderColorPicker(
          guiGraphics,
          ZetterRenderTypes.RenderMode.OK_HUE_SATURATION,
          this.parentScreen.getCurrentColor().getOkHsl(),
          this.getX() + COLOR_WHEEL_POSITION_X + 2,
          this.getY() + COLOR_WHEEL_POSITION_Y + 2,
          COLOR_WHEEL_WIDTH - 4,
          COLOR_WHEEL_HEIGHT - 4
      );
    } else {
      ZetterColorPickerRenderer.renderColorPicker(
          guiGraphics,
          ZetterRenderTypes.RenderMode.RGB_HUE_SATURATION,
          this.parentScreen.getCurrentColor().getHsl(),
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

  private boolean handleWheelInteraction(double mouseX, double mouseY) {
    int iMouseX = (int) mouseX;
    int iMouseY = (int) mouseY;

    float radius = (COLOR_WHEEL_WIDTH - 4) / 2.0f;

    if (
        isInRect(this.getX() + COLOR_WHEEL_POSITION_X + 2, this.getY() + COLOR_WHEEL_POSITION_Y + 2, COLOR_WHEEL_WIDTH - 4, COLOR_WHEEL_HEIGHT - 4, iMouseX, iMouseY)
            && Math.sqrt(Math.pow(iMouseX - (this.getX() + COLOR_WHEEL_POSITION_X + 2 + radius), 2) + Math.pow(iMouseY - (this.getY() + COLOR_WHEEL_POSITION_Y + 2 + radius), 2)) <= radius
    ) {
      Vector2f colorPosition = new Vector2f(
          (iMouseX - (this.getX() + COLOR_WHEEL_POSITION_X + 2 + radius)) / radius,
          (iMouseY - (this.getY() + COLOR_WHEEL_POSITION_Y + 2 + radius)) / radius
      );

      float hue = (float) (Math.atan2(colorPosition.y, colorPosition.x));

      if (hue < 0) {
        hue += (float) (2.0f * Math.PI);
      }

      hue = (float) (hue / (2.0f * Math.PI));

      float saturation = Math.min(1.0f, Math.max(0.0f, colorPosition.length()));

      if (this.parentScreen.getEaselState().colorSpace() == PaintingScreen.ColorSpace.okHSL) {
        this.parentScreen.setCurrentColor(
            Color.fromOkHsl(
                new Vector3f(
                    hue,
                    saturation,
                    this.parentScreen.getCurrentColor().getOkHsl().z
                )
            )
        );
      } else {
        this.parentScreen.setCurrentColor(
            Color.fromHsl(
                new Vector3f(
                    hue,
                    saturation,
                    this.parentScreen.getCurrentColor().getHsl().z
                )
            )
        );
      }

      return true;
    }

    return false;
  }

  @Override
  public boolean mouseClicked(double mouseX, double mouseY, int button) {
    if (!this.isMouseOver(mouseX, mouseY) || !this.isValidClickButton(button)) {
      return false;
    }

    if (this.handleWheelInteraction(mouseX, mouseY)) {
      return true;
    }

    if (this.paletteWidget.mouseClicked(mouseX, mouseY, button)) {
      return true;
    }

    if (this.wheelLightnessSlider.mouseClicked(mouseX, mouseY, button)) {
      return true;
    }

    return false;
  }

  @Override
  public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
    if (!this.active || !this.visible || !this.isValidClickButton(button)) {
      return false;
    }

    if (this.handleWheelInteraction(mouseX, mouseY)) {
      return true;
    }

    if (this.wheelLightnessSlider.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) {
      return true;
    }

    return false;
  }

  @Override
  public boolean mouseReleased(double mouseX, double mouseY, int button) {
    if (!this.active || !this.visible || !this.isValidClickButton(button)) {
      return false;
    }

    return this.wheelLightnessSlider.mouseReleased(mouseX, mouseY, button);
  }

  protected void renderCurrentColor(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    final int CURRENT_COLOR_U = 90;
    final int CURRENT_COLOR_V = 189;
    final int CURRENT_COLOR_WIDTH = 10;
    final int CURRENT_COLOR_HEIGHT = 10;

    final int COLOR_WHEEL_RADIUS = (COLOR_WHEEL_WIDTH - 4) / 2;

    int offsetX;
    int offsetY;

    final Color color = this.parentScreen.getCurrentColor();

    if (this.parentScreen.getEaselState().colorSpace() == PaintingScreen.ColorSpace.okHSL) {
      Vector3f okHsl = color.getOkHsl();
      offsetX = COLOR_WHEEL_RADIUS + (int) (Math.cos(okHsl.x * 2.0 * Math.PI) * okHsl.y * COLOR_WHEEL_RADIUS);
      offsetY = COLOR_WHEEL_RADIUS + (int) (Math.sin(okHsl.x * 2.0 * Math.PI) * okHsl.y * COLOR_WHEEL_RADIUS);
    } else {
      Vector3f hsl = this.parentScreen.getCurrentColor().getHsl();
      offsetX = COLOR_WHEEL_RADIUS + (int) (Math.cos(hsl.x * 2.0 * Math.PI) * hsl.y * COLOR_WHEEL_RADIUS);
      offsetY = COLOR_WHEEL_RADIUS + (int) (Math.sin(hsl.x * 2.0 * Math.PI) * hsl.y * COLOR_WHEEL_RADIUS);
    }

    guiGraphics.fill(
        this.getX() + COLOR_WHEEL_POSITION_X + 2 + offsetX - 3,
        this.getY() + COLOR_WHEEL_POSITION_Y + 2 + offsetY - 3,
        this.getX() + COLOR_WHEEL_POSITION_X + 2 + offsetX + 3,
        this.getY() + COLOR_WHEEL_POSITION_Y + 2 + offsetY + 3,
        color.getARGB()
    );

    guiGraphics.blit(
        PAINTING_WIDGETS_TEXTURE_RESOURCE,
        this.getX() + COLOR_WHEEL_POSITION_X + 2 + offsetX - 5,
        this.getY() + COLOR_WHEEL_POSITION_Y + 2 + offsetY - 5,
        CURRENT_COLOR_U,
        CURRENT_COLOR_V,
        10,
        10
    );

    guiGraphics.blit(PAINTING_WIDGETS_TEXTURE_RESOURCE, this.getX() + 150, this.getY() + 16, CURRENT_COLOR_U, CURRENT_COLOR_V, CURRENT_COLOR_WIDTH, CURRENT_COLOR_HEIGHT);
  }

  protected void renderLightnessVerticalSliderBackground(GuiGraphics guiGraphics, int x, int y, int width, int height, float value) {
    if (this.parentScreen.getEaselState().colorSpace() == PaintingScreen.ColorSpace.okHSL) {
      ZetterColorPickerRenderer.renderColorPicker(
          guiGraphics,
          ZetterRenderTypes.RenderMode.OK_LIGHTNESS_VERTICAL,
          this.parentScreen.getCurrentColor().getOkHsl(),
          x,
          y,
          width,
          height
      );
    } else {
      ZetterColorPickerRenderer.renderColorPicker(
          guiGraphics,
          ZetterRenderTypes.RenderMode.RGB_LIGHTNESS_VERTICAL,
          this.parentScreen.getCurrentColor().getHsl(),
          x,
          y,
          width,
          height
      );
    }

  }
}
