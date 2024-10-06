package me.dantaeusb.zetter.client.gui.painting.tool.bucket;

import me.dantaeusb.zetter.client.gui.PaintingScreen;
import me.dantaeusb.zetter.client.gui.easel.AbstractEaselWidget;
import me.dantaeusb.zetter.client.gui.easel.BlendingWidget;
import me.dantaeusb.zetter.client.gui.painting.AbstractPaintingWidget;
import me.dantaeusb.zetter.client.gui.painting.base.SliderWidget;
import me.dantaeusb.zetter.painting.parameters.BucketParameters;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.network.chat.Component;

public class BucketParametersWidget extends AbstractPaintingWidget implements Renderable {
  private final SliderWidget intensitySlider;

  public BucketParametersWidget(PaintingScreen parentScreen, int x, int y, int width, int height, Component title) {
    super(parentScreen, x, y, width, height, title);

    final int INTENSITY_POSITION_X = 0;
    final int INTENSITY_POSITION_Y = BlendingWidget.HEIGHT + 14;

    this.intensitySlider = new SliderWidget(
        parentScreen, this.getX() + INTENSITY_POSITION_X, this.getY() + INTENSITY_POSITION_Y,
        Component.translatable("container.zetter.painting.sliders.intensity"),
        this::updateIntensity, this::renderIntensityBackground, this::renderIntensityState
    );
  }

  public void updateIntensity(float percent) {
    BucketParameters parameters = this.parentScreen.getToolsParameters().getBucketParameters();
    parameters.setIntensity(1f + percent * 5f);
  }


  public void renderIntensityBackground(GuiGraphics guiGraphics, int x, int y, int width, int height, float value) {
    final int INTENSITY_BACKGROUND_U = 8;
    final int INTENSITY_BACKGROUND_V = 99;

    guiGraphics.blit(AbstractEaselWidget.EASEL_WIDGETS_TEXTURE_RESOURCE, x, y, INTENSITY_BACKGROUND_U, INTENSITY_BACKGROUND_V, width, height);
  }

  public void renderIntensityState(GuiGraphics guiGraphics, int x, int y, int width, int height, float value) {
    final int INTENSITY_STATE_U = 8;
    final int INTENSITY_STATE_V = 99;

  }

  @Override
  protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    this.intensitySlider.render(guiGraphics, mouseX, mouseY, partialTick);
  }
}
