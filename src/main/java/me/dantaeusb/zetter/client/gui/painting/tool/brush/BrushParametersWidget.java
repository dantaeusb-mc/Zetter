package me.dantaeusb.zetter.client.gui.painting.tool.brush;

import me.dantaeusb.zetter.client.gui.PaintingScreen;
import me.dantaeusb.zetter.client.gui.painting.base.OptionsWidget;
import me.dantaeusb.zetter.client.gui.painting.base.SliderWidget;
import me.dantaeusb.zetter.client.gui.painting.tool.AbstractToolParametersWidget;
import me.dantaeusb.zetter.core.tools.Color;
import me.dantaeusb.zetter.painting.parameters.BrushParameters;
import me.dantaeusb.zetter.painting.pipes.BlendingPipe;
import me.dantaeusb.zetter.painting.pipes.DitheringPipe;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.network.chat.Component;

public class BrushParametersWidget extends AbstractToolParametersWidget implements Renderable {
  private final SliderWidget intensitySlider;
  private final SliderWidget sizeSlider;
  private final OptionsWidget<DitheringPipe.DitheringOption> ditheringWidget;
  private final OptionsWidget<BlendingPipe.BlendingOption> blendingWidget;

  private final static int OPTIONS_POSITION_Y = SLIDER_DISTANCE_GAP * 3 + SliderWidget.HORIZONTAL_HEIGHT * 2 + OPTIONS_LABEL_GAP;

  public BrushParametersWidget(PaintingScreen parentScreen, int x, int y, int width, int height, Component title) {
    super(parentScreen, x, y, width, height, title, "brush");

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
        this::renderHandlerState
    );
    this.addWidget(this.intensitySlider);

    this.sizeSlider = new SliderWidget(
        parentScreen,
        this.getX() + SLIDER_POSITION_X,
        this.getY() + SIZE_POSITION_Y,
        Component.translatable("container.zetter.painting.sliders.size"),
        this::getSize,
        this::updateSize,
        this::renderSizeBackground,
        this::renderHandlerState
    );
    this.addWidget(this.sizeSlider);

    this.ditheringWidget = this.createDitheringWidget(
        this.getX() + DITHERING_POSITION_X,
        this.getY() + OPTIONS_POSITION_Y,
        () -> parentScreen.getToolsParameters().getBrushParameters()
    );
    this.addWidget(this.ditheringWidget);

    this.blendingWidget = this.createBlendingWidget(
        this.getX() + BLENDING_POSITION_X,
        this.getY() + OPTIONS_POSITION_Y,
        () -> parentScreen.getToolsParameters().getBrushParameters()
    );
    this.addWidget(this.blendingWidget);
  }

  private float getIntensity() {
    BrushParameters parameters = this.parentScreen.getToolsParameters().getBrushParameters();
    return parameters.getIntensity();
  }

  private void updateIntensity(float percent) {
    BrushParameters parameters = this.parentScreen.getToolsParameters().getBrushParameters();
    parameters.setIntensity(percent);
  }

  /**
   * Bounds are asked for every time instead of being read once, as bigger
   * canvas resolutions are going to allow for bigger sizes
   */
  private float getMinSize() {
    return BrushParameters.MIN_SIZE;
  }

  private float getMaxSize() {
    return BrushParameters.MAX_SIZE;
  }

  @Override
  protected int getSizeNotches() {
    return Math.round(this.getMaxSize() - this.getMinSize()) + 1;
  }

  /**
   * Slider works with a 0 to 1 position, size is measured in pixels
   */
  private float getSize() {
    BrushParameters parameters = this.parentScreen.getToolsParameters().getBrushParameters();
    return (parameters.getSize() - this.getMinSize()) / (this.getMaxSize() - this.getMinSize());
  }

  /**
   * Notches only mark the whole pixel sizes: brush falls off towards its edge,
   * so sizes in between are painted differently and the slider does not snap
   */
  private void updateSize(float percent) {
    BrushParameters parameters = this.parentScreen.getToolsParameters().getBrushParameters();
    parameters.setSize(this.getMinSize() + percent * (this.getMaxSize() - this.getMinSize()));
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

    this.renderOptions(guiGraphics, mouseX, mouseY, partialTick, OPTIONS_POSITION_Y, this.ditheringWidget, this.blendingWidget);
  }
}
