package me.dantaeusb.zetter.client.gui.painting.tool.pencil;

import me.dantaeusb.zetter.client.gui.PaintingScreen;
import me.dantaeusb.zetter.client.gui.painting.AbstractPaintingWidget;
import me.dantaeusb.zetter.client.gui.painting.base.SliderWidget;
import me.dantaeusb.zetter.core.tools.Color;
import me.dantaeusb.zetter.painting.parameters.PencilParameters;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.network.chat.Component;

public class PencilParametersWidget extends AbstractPaintingWidget implements Renderable {
  private final static int SLIDER_POSITION_X = 5;
  private final static int SLIDER_DISTANCE_GAP = 14;

  private final SliderWidget intensitySlider;
  private final SliderWidget sizeSlider;

  public PencilParametersWidget(PaintingScreen parentScreen, int x, int y, int width, int height, Component title) {
    super(parentScreen, x, y, width, height, title);

    final int INTENSITY_POSITION_Y = 0;//BlendingWidget.HEIGHT + 14;

    final int SIZE_POSITION_Y = SLIDER_DISTANCE_GAP + SliderWidget.HORIZONTAL_HEIGHT;

    this.intensitySlider = new SliderWidget(
        parentScreen,
        this.getX() + SLIDER_POSITION_X,
        this.getY() + INTENSITY_POSITION_Y,
        Component.translatable("container.zetter.painting.sliders.intensity"),
        this::getIntensity,
        this::updateIntensity,
        this::renderIntensityBackground,
        this::renderIntensityState
    );

    this.sizeSlider = new SliderWidget(
        parentScreen,
        this.getX() + SLIDER_POSITION_X,
        this.getY() + SIZE_POSITION_Y,
        Component.translatable("container.zetter.painting.sliders.size"),
        this::getSize,
        this::updateSize,
        this::renderIntensityBackground,
        this::renderIntensityState
    );
  }

  private float getIntensity() {
    PencilParameters parameters = this.parentScreen.getToolsParameters().getPencilParameters();
    return parameters.getIntensity();
  }

  private void updateIntensity(float percent) {
    PencilParameters parameters = this.parentScreen.getToolsParameters().getPencilParameters();
    parameters.setIntensity(percent);
  }

  private float getSize() {
    PencilParameters parameters = this.parentScreen.getToolsParameters().getPencilParameters();
    return parameters.getSize();
  }

  private void updateSize(float percent) {
    PencilParameters parameters = this.parentScreen.getToolsParameters().getPencilParameters();
    parameters.setSize(1f + percent * 5f);
  }


  public void renderIntensityBackground(GuiGraphics guiGraphics, int x, int y, int width, int height, float value) {
    final int INTENSITY_BACKGROUND_U = 8;
    final int INTENSITY_BACKGROUND_V = 99;

    guiGraphics.blit(AbstractPaintingWidget.PAINTING_WIDGETS_TEXTURE_RESOURCE, x, y, INTENSITY_BACKGROUND_U, INTENSITY_BACKGROUND_V, width, height);
  }

  public void renderIntensityState(GuiGraphics guiGraphics, int x, int y, int width, int height, float value) {
    final int INTENSITY_STATE_U = 8;
    final int INTENSITY_STATE_V = 99;
  }

  @Override
  protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    guiGraphics.drawString(
        this.parentScreen.getFont(),
        this.intensitySlider.getMessage().getString(),
        this.getX() + SLIDER_POSITION_X + 3,
        this.getY() + SliderWidget.HORIZONTAL_HEIGHT * 2 + SLIDER_DISTANCE_GAP * 2 + 4,
        Color.DARK_GRAY.getARGB(), false
    );

    this.intensitySlider.render(guiGraphics, mouseX, mouseY, partialTick);

    guiGraphics.drawString(
        this.parentScreen.getFont(),
        this.sizeSlider.getMessage().getString(),
        this.getX() + SLIDER_POSITION_X + 3,
        this.getY() + SliderWidget.HORIZONTAL_HEIGHT * 2 + SLIDER_DISTANCE_GAP * 2 + 4,
        Color.DARK_GRAY.getARGB(), false
    );

    this.sizeSlider.render(guiGraphics, mouseX, mouseY, partialTick);
  }
}
