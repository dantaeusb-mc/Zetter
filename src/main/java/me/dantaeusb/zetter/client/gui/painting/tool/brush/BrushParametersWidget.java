package me.dantaeusb.zetter.client.gui.painting.tool.brush;

import me.dantaeusb.zetter.client.gui.PaintingScreen;
import me.dantaeusb.zetter.client.gui.painting.AbstractPaintingWidget;
import me.dantaeusb.zetter.client.gui.painting.AbstractPaintingGroupWidget;
import me.dantaeusb.zetter.client.gui.painting.base.SliderWidget;
import me.dantaeusb.zetter.core.tools.Color;
import me.dantaeusb.zetter.painting.parameters.BrushParameters;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.network.chat.Component;

public class BrushParametersWidget extends AbstractPaintingGroupWidget implements Renderable {
  private final static int SLIDER_POSITION_X = 5;
  private final static int SLIDER_DISTANCE_GAP = 14;

  private final SliderWidget intensitySlider;
  private final SliderWidget sizeSlider;

  public BrushParametersWidget(PaintingScreen parentScreen, int x, int y, int width, int height, Component title) {
    super(parentScreen, x, y, width, height, title);

    final int INTENSITY_POSITION_Y = SLIDER_DISTANCE_GAP;
    final int SIZE_POSITION_Y = INTENSITY_POSITION_Y + SLIDER_DISTANCE_GAP + SliderWidget.HORIZONTAL_HEIGHT;

    this.intensitySlider = new SliderWidget(
        parentScreen,
        this.getX() + SLIDER_POSITION_X,
        this.getY() + INTENSITY_POSITION_Y,
        Component.translatable("container.zetter.painting.sliders.intensity"),
        this::getIntensity,
        this::updateIntensity,
        this::renderIntensityBackground,
        this::renderIntensityHandlerState
    );
    this.addWidget(this.intensitySlider);

    this.sizeSlider = new SliderWidget(
        parentScreen,
        this.getX() + SLIDER_POSITION_X,
        this.getY() + SIZE_POSITION_Y,
        Component.translatable("container.zetter.painting.sliders.size"),
        this::getSize,
        this::updateSize,
        this::renderIntensityBackground,
        this::renderIntensityHandlerState
    );
    this.addWidget(this.sizeSlider);
  }

  private float getIntensity() {
    BrushParameters parameters = this.parentScreen.getToolsParameters().getBrushParameters();
    return parameters.getIntensity();
  }

  private void updateIntensity(float percent) {
    BrushParameters parameters = this.parentScreen.getToolsParameters().getBrushParameters();
    parameters.setIntensity(percent);
  }

  private float getSize() {
    BrushParameters parameters = this.parentScreen.getToolsParameters().getBrushParameters();
    return parameters.getSize();
  }

  private void updateSize(float percent) {
    BrushParameters parameters = this.parentScreen.getToolsParameters().getBrushParameters();
    parameters.setSize(1f + percent * 5f);
  }


  public void renderIntensityBackground(GuiGraphics guiGraphics, int x, int y, int width, int height, float value) {
    final int INTENSITY_BACKGROUND_U = 102;
    final int INTENSITY_BACKGROUND_V = 139;

    guiGraphics.blit(AbstractPaintingWidget.PAINTING_WIDGETS_TEXTURE_RESOURCE, x, y, INTENSITY_BACKGROUND_U, INTENSITY_BACKGROUND_V, width, height);
  }

  public void renderIntensityHandlerState(GuiGraphics guiGraphics, int x, int y, int width, int height, float value) {

  }

  @Override
  protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    guiGraphics.drawString(
        this.parentScreen.getFont(),
        this.intensitySlider.getMessage().getString(),
        this.getX() + SLIDER_POSITION_X,
        this.getY() + 4,
        Color.DARK_GRAY.getARGB(), false
    );

    this.intensitySlider.render(guiGraphics, mouseX, mouseY, partialTick);

    guiGraphics.drawString(
        this.parentScreen.getFont(),
        this.sizeSlider.getMessage().getString(),
        this.getX() + SLIDER_POSITION_X,
        this.getY() + SliderWidget.HORIZONTAL_HEIGHT + SLIDER_DISTANCE_GAP + 4,
        Color.DARK_GRAY.getARGB(), false
    );

    this.sizeSlider.render(guiGraphics, mouseX, mouseY, partialTick);
  }
}
