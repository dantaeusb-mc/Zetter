package me.dantaeusb.zetter.client.gui.painting.tool.bucket;

import me.dantaeusb.zetter.client.gui.PaintingScreen;
import me.dantaeusb.zetter.client.gui.painting.base.SliderWidget;
import me.dantaeusb.zetter.client.gui.painting.tool.AbstractToolParametersWidget;
import me.dantaeusb.zetter.core.tools.Color;
import me.dantaeusb.zetter.painting.parameters.BucketParameters;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.network.chat.Component;

public class BucketParametersWidget extends AbstractToolParametersWidget implements Renderable {
  private final SliderWidget intensitySlider;

  public BucketParametersWidget(PaintingScreen parentScreen, int x, int y, int width, int height, Component title) {
    super(parentScreen, x, y, width, height, title);

    final int INTENSITY_POSITION_Y = SLIDER_DISTANCE_GAP;

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
  }

  private float getIntensity() {
    BucketParameters parameters = this.parentScreen.getToolsParameters().getBucketParameters();
    return parameters.getIntensity();
  }

  private void updateIntensity(float percent) {
    BucketParameters parameters = this.parentScreen.getToolsParameters().getBucketParameters();
    parameters.setIntensity(percent);
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
  }
}
