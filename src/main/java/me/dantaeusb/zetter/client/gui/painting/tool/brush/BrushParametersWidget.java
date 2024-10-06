package me.dantaeusb.zetter.client.gui.painting.tool.brush;

import me.dantaeusb.zetter.client.gui.PaintingScreen;
import me.dantaeusb.zetter.client.gui.easel.AbstractEaselWidget;
import me.dantaeusb.zetter.client.gui.easel.BlendingWidget;
import me.dantaeusb.zetter.client.gui.painting.AbstractPaintingWidget;
import me.dantaeusb.zetter.client.gui.painting.base.SliderWidget;
import me.dantaeusb.zetter.painting.parameters.BrushParameters;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.network.chat.Component;

public class BrushParametersWidget extends AbstractPaintingWidget implements Renderable {
  private final SliderWidget intensitySlider;
  private final SliderWidget sizeSlider;

  public BrushParametersWidget(PaintingScreen parentScreen, int x, int y, int width, int height, Component title) {
    super(parentScreen, x, y, width, height, title);

    final int INTENSITY_POSITION_X = 0;
    final int INTENSITY_POSITION_Y = BlendingWidget.HEIGHT + 14;

    final int SIZE_POSITION_X = 0;
    final int SIZE_POSITION_Y = 67;

    this.intensitySlider = new SliderWidget(
        parentScreen, this.getX() + INTENSITY_POSITION_X, this.getY() + INTENSITY_POSITION_Y,
        Component.translatable("container.zetter.painting.sliders.intensity"),
        this::updateIntensity, this::renderIntensityBackground, this::renderIntensityHandlerState
    );

    this.sizeSlider = new SliderWidget(
        parentScreen, this.getX() + SIZE_POSITION_X, this.getY() + SIZE_POSITION_Y,
        Component.translatable("container.zetter.painting.sliders.size"),
        this::updateSize, this::renderIntensityBackground, this::renderIntensityHandlerState
    );
  }

  public void updateIntensity(float percent) {
    BrushParameters parameters = this.parentScreen.getToolsParameters().getBrushParameters();
    parameters.setIntensity(percent);
  }

  public void updateSize(float percent) {
    BrushParameters parameters = this.parentScreen.getToolsParameters().getBrushParameters();
    parameters.setSize(1f + percent * 5f);
  }


  public void renderIntensityBackground(GuiGraphics guiGraphics, int x, int y, int width, int height, float value) {
    final int INTENSITY_BACKGROUND_U = 8;
    final int INTENSITY_BACKGROUND_V = 99;

    guiGraphics.blit(AbstractEaselWidget.EASEL_WIDGETS_TEXTURE_RESOURCE, x, y, INTENSITY_BACKGROUND_U, INTENSITY_BACKGROUND_V, width, height);
  }

  public void renderIntensityHandlerState(GuiGraphics guiGraphics, int x, int y, int width, int height, float value) {

  }

  @Override
  protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    this.intensitySlider.render(guiGraphics, mouseX, mouseY, partialTick);
    this.sizeSlider.render(guiGraphics, mouseX, mouseY, partialTick);
  }
}
