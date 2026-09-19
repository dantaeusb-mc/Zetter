package me.dantaeusb.zetter.client.gui.painting.tool.pencil;

import me.dantaeusb.zetter.client.gui.PaintingScreen;
import me.dantaeusb.zetter.client.gui.painting.base.SliderWidget;
import me.dantaeusb.zetter.client.gui.painting.tool.AbstractToolParametersWidget;
import me.dantaeusb.zetter.core.tools.Color;
import me.dantaeusb.zetter.painting.parameters.PencilParameters;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.network.chat.Component;

public class PencilParametersWidget extends AbstractToolParametersWidget implements Renderable {
    private final SliderWidget intensitySlider;
    private final SliderWidget sizeSlider;

    public PencilParametersWidget(PaintingScreen parentScreen, int x, int y, int width, int height, Component title) {
        super(parentScreen, x, y, width, height, title, "pencil");

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
    }

    private float getIntensity() {
        PencilParameters parameters = this.parentScreen.getToolsParameters().getPencilParameters();
        return parameters.getIntensity();
    }

    private void updateIntensity(float percent) {
        PencilParameters parameters = this.parentScreen.getToolsParameters().getPencilParameters();
        parameters.setIntensity(percent);
    }

    private float getMinSize() {
        return PencilParameters.MIN_SIZE;
    }

    private float getMaxSize() {
        return PencilParameters.MAX_SIZE;
    }

    @Override
    protected int getSizeNotches() {
        return Math.round(this.getMaxSize() - this.getMinSize()) + 1;
    }

    private float getSize() {
        PencilParameters parameters = this.parentScreen.getToolsParameters().getPencilParameters();
        return (parameters.getSize() - this.getMinSize()) / (this.getMaxSize() - this.getMinSize());
    }

    /**
     * Pencil picks its shape by the rounded size, so the slider stops at whole pixels
     */
    private void updateSize(float percent) {
        PencilParameters parameters = this.parentScreen.getToolsParameters().getPencilParameters();
        parameters.setSize(Math.round(this.getMinSize() + percent * (this.getMaxSize() - this.getMinSize())));
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
