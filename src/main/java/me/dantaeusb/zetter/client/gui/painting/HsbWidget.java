package me.dantaeusb.zetter.client.gui.painting;

import com.mojang.blaze3d.systems.RenderSystem;
import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.client.gui.PaintingScreen;
import com.mojang.blaze3d.vertex.PoseStack;
import me.dantaeusb.zetter.core.tools.Color;
import net.minecraft.client.gui.components.Widget;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;

import javax.annotation.Nullable;
import java.util.function.BiFunction;

public class HsbWidget extends AbstractPaintingWidget implements Widget {
    final static int SLIDER_WIDTH = 150;
    final static int SLIDER_HEIGHT = 9;

    final static int SLIDER_DISTANCE = 5; // distance between sliders

    final static int WIDTH = SLIDER_WIDTH;
    final static int HEIGHT = SLIDER_HEIGHT + (SLIDER_DISTANCE + SLIDER_HEIGHT) * 2;

    private float sliderHuePercent = 0.0f;
    private float sliderSaturationPercent = 0.0f;
    private float sliderValuePercent = 0.0f;
    private Integer sliderDraggingIndex;

    public HsbWidget(PaintingScreen parentScreen, int x, int y) {
        super(parentScreen, x, y, WIDTH, HEIGHT, new TranslatableComponent("container.zetter.painting.sliders"));
    }

    @Override
    public @Nullable Component getTooltip(int mouseX, int mouseY) {
        return null;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int iMouseX = (int) mouseX;
        int iMouseY = (int) mouseY;

        if (this.isMouseOver(mouseX, mouseY)) {
            this.handleSliderInteraction(iMouseX, iMouseY);
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.sliderDraggingIndex != null) {
            int iMouseX = (int) mouseX;
            int iMouseY = (int) mouseY;

            this.handleSliderInteraction(iMouseX, iMouseY, this.sliderDraggingIndex);
            return true;
        }

        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.sliderDraggingIndex != null) {
            // If we were changing palette colors, sync them with server
            this.parentScreen.getMenu().sendPaletteUpdatePacket();
        }

        this.sliderDraggingIndex = null;

        return super.mouseReleased(mouseX, mouseY, button);
    }

    public void render(PoseStack matrixStack) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, AbstractPaintingWidget.PAINTING_WIDGETS_RESOURCE);

        drawSliderBackground(matrixStack, 0, this.isDraggingSlider(0));
        drawSliderBackground(matrixStack, 1, this.isDraggingSlider(1));
        drawSliderBackground(matrixStack, 2, this.isDraggingSlider(2));

        drawSliderForeground(matrixStack, 0, HsbWidget::getHue, this.isDraggingSlider(0));
        drawSliderForeground(matrixStack, 1, HsbWidget::getSaturation, this.isDraggingSlider(1));
        drawSliderForeground(matrixStack, 2, HsbWidget::getValue, this.isDraggingSlider(2));

        drawHandler(matrixStack, 0, this.sliderHuePercent, this.isDraggingSlider(0));
        drawHandler(matrixStack, 1, this.sliderSaturationPercent, this.isDraggingSlider(1));
        drawHandler(matrixStack, 2, this.sliderValuePercent, this.isDraggingSlider(2));
    }

    protected void drawSliderBackground(PoseStack matrixStack, int verticalOffset, boolean active) {
        final int SLIDER_POSITION_U = 5;
        final int SLIDER_POSITION_V = 80;

        int top = this.y + (verticalOffset * (SLIDER_HEIGHT + SLIDER_DISTANCE));

        int sliderV = SLIDER_POSITION_V;

        if (active) {
            sliderV += SLIDER_HEIGHT;
        }

        this.blit(matrixStack, this.x, top, SLIDER_POSITION_U, sliderV, SLIDER_WIDTH, SLIDER_HEIGHT);
    }

    protected void drawSliderForeground(PoseStack matrixStack, int verticalOffset, BiFunction<float[], Float, Integer> getColorLambda, boolean active) {
        int sliderContentGlobalLeft = this.x + 3;
        int sliderContentGlobalTop = this.y + (verticalOffset * (SLIDER_HEIGHT + SLIDER_DISTANCE)) + 3;

        int sliderContentWidth = SLIDER_WIDTH - 6;
        int sliderContentHeight = 3;

        if (active) {
            sliderContentGlobalTop -= 2;
            sliderContentHeight += 4;
        }

        Color currentColor = new Color(parentScreen.getMenu().getCurrentColor());
        float[] currentColorHSB = Color.RGBtoHSB(currentColor.getRed(), currentColor.getGreen(), currentColor.getBlue(), null);

        // We're not doing gradient because it's too smooth for Minecraft :)
        for (int i = 0; i < sliderContentWidth; i++) {
            int color = getColorLambda.apply(currentColorHSB, (float) i / sliderContentWidth);

            fill(matrixStack, sliderContentGlobalLeft + i, sliderContentGlobalTop, sliderContentGlobalLeft + i + 1, sliderContentGlobalTop + sliderContentHeight, color);
        }
    }

    protected static Integer getHue(float[] inColorHSB, float percent) {
        return Color.HSBtoRGB(percent, 1.0f, 1.0f);
    }

    protected static Integer getSaturation(float[] inColorHSB, float percent) {
        return Color.HSBtoRGB(inColorHSB[0], 1.0f - percent, inColorHSB[2]);
    }

    protected static Integer getValue(float[] inColorHSB, float percent) {
        return Color.HSBtoRGB(inColorHSB[0], inColorHSB[1], 1.0f - percent);
    }

    public void updateSlidersWithCurrentColor() {
        Color currentColor = new Color(parentScreen.getMenu().getCurrentColor());
        float[] currentColorHSB = currentColor.toHSB();

        this.sliderHuePercent = currentColorHSB[0];
        this.sliderSaturationPercent = 1.0f - currentColorHSB[1];
        this.sliderValuePercent = 1.0f - currentColorHSB[2];
    }

    protected void handleSliderInteraction(final int mouseX, final int mouseY) {
        this.handleSliderInteraction(mouseX, mouseY, null);
    }

    protected boolean isDraggingSlider(int index) {
        return this.sliderDraggingIndex != null && this.sliderDraggingIndex == index;
    }

    /**
     *
     * @param mouseX
     * @param mouseY
     * @param sliderIndex do not lookup if not null, just update slider based on mouse position - should be provided when dragging
     */
    protected void handleSliderInteraction(final int mouseX, final int mouseY, @Nullable Integer sliderIndex) {
        if (sliderIndex == null) {
            for (int i = 0; i < 3; i++) {
                int top = this.y + (i * (SLIDER_HEIGHT + SLIDER_DISTANCE));

                if (PaintingScreen.isInRect(this.x, top, SLIDER_WIDTH, SLIDER_HEIGHT, mouseX, mouseY)) {
                    sliderIndex = i;
                    break;
                }
            }

            // Should only happen if clicked on border
            if (sliderIndex == null) {
                Zetter.LOG.warn("Cannot find slider!");
                return;
            }
        }

        this.sliderDraggingIndex = sliderIndex;

        float percent = (float) (mouseX - this.x - 3) / (SLIDER_WIDTH - 6);
        float percentC = Mth.clamp(percent, 0.0f, 1.0f);

        this.updateSliderPosition(sliderIndex, percentC);
    }

    protected void updateSliderPosition(int sliderIndex, float percent) {
        Color currentColor = new Color(this.parentScreen.getMenu().getCurrentColor());
        float[] currentColorHSB = Color.RGBtoHSB(currentColor.getRed(), currentColor.getGreen(), currentColor.getBlue(), null);

        int newColor;

        switch(sliderIndex) {
            case 0:
                newColor = Color.HSBtoRGB(percent, currentColorHSB[1], currentColorHSB[2]);
                this.sliderHuePercent = percent;
                break;
            case 1:
                newColor = Color.HSBtoRGB(this.sliderHuePercent, 1.0f - percent, currentColorHSB[2]);
                this.sliderSaturationPercent = percent;
                break;
            case 2:
                newColor = Color.HSBtoRGB(this.sliderHuePercent, currentColorHSB[1], 1.0f - percent);
                this.sliderValuePercent = percent;
                break;
            default:
                newColor = this.parentScreen.getMenu().getCurrentColor();
                break;
        }

        this.parentScreen.getMenu().setPaletteColor(newColor);
    }

    /**
     * Handlers
     */

    protected void drawHandler(PoseStack matrixStack, int verticalOffset, float percent, boolean active) {
        final int HANDLER_POSITION_U = 0;
        final int HANDLER_POSITION_V = 80;
        final int HANDLER_WIDTH = 5;
        final int HANDLER_HEIGHT = 11;

        int sliderContentWidth = SLIDER_WIDTH - 6;

        int sliderGlobalLeft = this.x + (int) (sliderContentWidth * percent) + 3 - 2;
        int sliderGlobalTop = this.y + (verticalOffset * (SLIDER_HEIGHT + SLIDER_DISTANCE)) - 1;

        int sliderV = HANDLER_POSITION_V;

        if (active) {
            sliderV += HANDLER_HEIGHT;
        }

        this.blit(matrixStack, sliderGlobalLeft, sliderGlobalTop, HANDLER_POSITION_U, sliderV, HANDLER_WIDTH, HANDLER_HEIGHT);
    }
}
