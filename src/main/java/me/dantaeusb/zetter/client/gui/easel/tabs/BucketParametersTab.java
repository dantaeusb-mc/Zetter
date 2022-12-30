package me.dantaeusb.zetter.client.gui.easel.tabs;

import com.mojang.blaze3d.vertex.PoseStack;
import me.dantaeusb.zetter.client.gui.EaselScreen;
import me.dantaeusb.zetter.client.gui.easel.BlendingWidget;
import me.dantaeusb.zetter.client.gui.easel.SliderWidget;
import me.dantaeusb.zetter.core.tools.Color;
import me.dantaeusb.zetter.painting.parameters.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;

public class BucketParametersTab extends AbstractTab {
    private final BlendingWidget blendingWidget;
    private final SliderWidget intensityWidget;

    public BucketParametersTab(EaselScreen parentScreen, int windowX, int windowY) {
        super(parentScreen, windowX, windowY, new TranslatableComponent("container.zetter.painting.tabs.parameters"));

        final int BLENDING_POSITION_X = 0;
        final int BLENDING_POSITION_Y = 1;

        final int INTENSITY_POSITION_X = 0;
        final int INTENSITY_POSITION_Y = BlendingWidget.HEIGHT + 14;

        this.blendingWidget = new BlendingWidget(this.parentScreen, this.x + BLENDING_POSITION_X, this.y + BLENDING_POSITION_Y);
        this.intensityWidget = new SliderWidget(
                parentScreen, this.x + INTENSITY_POSITION_X, this.y + INTENSITY_POSITION_Y,
                new TranslatableComponent("container.zetter.painting.sliders.intensity"),
                this::updateIntensity, this::renderIntensityBackground, this::renderIntensityForeground
        );

        this.addTabWidget(this.blendingWidget);
        this.addTabWidget(this.intensityWidget);
    }

    public void update(AbstractToolParameters parameters) {
        if (parameters instanceof IntensityParameterHolder) {
            this.intensityWidget.setSliderState(((IntensityParameterHolder) parameters).getIntensity());
        }
    }

    @Override
    public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        if (this.visible) {
            fill(matrixStack, this.x, this.y, this.x + this.width, this.y + this.height, Color.SCREEN_GRAY.getRGB());

            if (this.parentScreen.getMenu().getCurrentToolParameters() instanceof BlendingParameterHolder) {
                this.blendingWidget.render(matrixStack);
            }

            if (this.parentScreen.getMenu().getCurrentToolParameters() instanceof IntensityParameterHolder) {
                this.intensityWidget.render(matrixStack);
            }
        }
    }

    public void renderLabels(PoseStack matrixStack, int mouseX, int mouseY) {
        if (this.visible) {
            this.parentScreen.getFont().draw(
                    matrixStack, this.intensityWidget.getMessage(),
                    (float) this.x - this.parentScreen.getGuiLeft(), (float) this.y - this.parentScreen.getGuiTop() + BlendingWidget.HEIGHT + 4,
                    Color.DARK_GRAY.getRGB()
            );
        }

        super.renderLabels(matrixStack, mouseX, mouseY);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.active) {
            int iMouseX = (int) mouseX;
            int iMouseY = (int) mouseY;

            if (this.isMouseOver(mouseX, mouseY)) {
                this.blendingWidget.mouseClicked(iMouseX, iMouseY, button);
                this.intensityWidget.mouseClicked(iMouseX, iMouseY, button);
            }
        }

        return false;
    }

    public void renderIntensityBackground(PoseStack matrixStack, int x, int y, int width, int height) {
        final int INTENSITY_BACKGROUND_U = 8;
        final int INTENSITY_BACKGROUND_V = 99;

        blit(matrixStack, x, y, INTENSITY_BACKGROUND_U, INTENSITY_BACKGROUND_V, width, height);
    }

    public int renderIntensityForeground(float percent) {
        final int alpha = (((int) (255 * percent)) << 24) | 0x00FFFFFF;
        return alpha & this.parentScreen.getMenu().getCurrentColor();
    }

    public void updateIntensity(float percent) {
        AbstractToolParameters parameters = this.parentScreen.getMenu().getCurrentToolParameters();

        if (parameters instanceof IntensityParameterHolder) {
            ((IntensityParameterHolder) parameters).setIntensity(percent);
        }
    }

    public void updateSize(float percent) {
        AbstractToolParameters parameters = this.parentScreen.getMenu().getCurrentToolParameters();

        if (parameters instanceof SizeParameterHolder) {
            ((SizeParameterHolder) parameters).setSize(1f + percent * 5f);
        }
    }

    /**
     * Unfortunately this event is not passed to children
     * @param mouseX
     * @param mouseY
     * @param button
     * @param dragX
     * @param dragY
     * @return
     */
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.active) {
            this.intensityWidget.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }

        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.active) {
            this.intensityWidget.mouseReleased(mouseX, mouseY, button);
        }

        return super.mouseReleased(mouseX, mouseY, button);
    }
}
