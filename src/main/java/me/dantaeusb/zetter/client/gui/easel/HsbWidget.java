package me.dantaeusb.zetter.client.gui.easel;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import me.dantaeusb.zetter.client.gui.EaselScreen;
import me.dantaeusb.zetter.client.gui.easel.tabs.AbstractTab;
import me.dantaeusb.zetter.core.tools.Color;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class HsbWidget extends AbstractPaintingWidget implements Renderable {
    final static int SLIDER_DISTANCE = 5; // distance between sliders

    final static int WIDTH = AbstractTab.WIDTH;
    final static int HEIGHT = SliderWidget.HEIGHT + (SLIDER_DISTANCE + SliderWidget.HEIGHT) * 2;

    private final SliderWidget hueSlider;

    private final SliderWidget saturationSlider;

    private final SliderWidget brightnessSlider;

    private final List<SliderWidget> sliders = new ArrayList<>();

    public HsbWidget(EaselScreen parentScreen, int x, int y) {
        super(parentScreen, x, y, WIDTH, HEIGHT, Component.translatable("container.zetter.painting.sliders"));

        final int sliderOffset = WIDTH - SliderWidget.WIDTH;

        this.hueSlider = new SliderWidget(
                parentScreen, x + sliderOffset, y,
                Component.translatable("container.zetter.painting.sliders.hue"),
                this::updateHue, null, this::renderHueForeground
        );

        this.saturationSlider = new SliderWidget(
                parentScreen, x + sliderOffset, y + SliderWidget.HEIGHT + SLIDER_DISTANCE,
                Component.translatable("container.zetter.painting.sliders.saturation"),
                this::updateSaturation, null, this::renderSaturationForeground
        );

        this.brightnessSlider = new SliderWidget(
                parentScreen, x + sliderOffset, y + (SliderWidget.HEIGHT + SLIDER_DISTANCE) * 2,
                Component.translatable("container.zetter.painting.sliders.brightness"),
                this::updateBrightness, null, this::renderBrightnessForeground
        );

        this.sliders.add(this.hueSlider);
        this.sliders.add(this.saturationSlider);
        this.sliders.add(this.brightnessSlider);
    }

    public void updateColor(int color) {
        final Color newColor = new Color(color);
        float[] hsb = newColor.toHSB();

        this.hueSlider.setSliderState(hsb[0]);
        this.saturationSlider.setSliderState(hsb[1]);
        this.brightnessSlider.setSliderState(hsb[2]);
    }

    @Override
    public @Nullable Component getTooltip(int mouseX, int mouseY) {
        return null;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean result = this.hueSlider.mouseClicked(mouseX, mouseY, button) ||
                         this.saturationSlider.mouseClicked(mouseX, mouseY, button) ||
                         this.brightnessSlider.mouseClicked(mouseX, mouseY, button);

        this.setFocused(result);

        return result;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        this.hueSlider.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        this.saturationSlider.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        this.brightnessSlider.mouseDragged(mouseX, mouseY, button, dragX, dragY);

        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.hueSlider.mouseReleased(mouseX, mouseY, button);
        this.saturationSlider.mouseReleased(mouseX, mouseY, button);
        this.brightnessSlider.mouseReleased(mouseX, mouseY, button);

        return super.mouseReleased(mouseX, mouseY, button);
    }

    public void render(PoseStack matrixStack) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, AbstractPaintingWidget.PAINTING_WIDGETS_RESOURCE);

        for (SliderWidget slider : this.sliders) {
            slider.render(matrixStack);
        }
    }

    public void renderLabels(PoseStack matrixStack, int posX, int posY) {
        int i = 0;

        for (SliderWidget slider : this.sliders) {
            this.parentScreen.getFont().draw(
                    matrixStack,
                    slider.getMessage().getString().substring(0, 1).concat("."),
                    this.getX() - this.parentScreen.getGuiLeft(),
                    this.getY() + (SliderWidget.HEIGHT + SLIDER_DISTANCE) * i++ - this.parentScreen.getGuiTop() + 1,
                    Color.DARK_GRAY.getRGB()
            );
        }
    }

    public void updateSlidersWithCurrentColor() {
        Color currentColor = new Color(parentScreen.getMenu().getCurrentColor());
        float[] currentColorHSB = currentColor.toHSB();

        //this.hueSlider;
        //this.sliderSaturationPercent = 1.0f - currentColorHSB[1];
        //this.sliderValuePercent = 1.0f - currentColorHSB[2];
    }

    /*
     * Sliders lambdas
     */

    public int renderHueForeground(float percent) {
        return Color.HSBtoRGB(percent, 1.0f, 1.0f);
    }

    public void updateHue(float percent) {
        this.pushColorUpdate();
    }

    public int renderSaturationForeground(float percent) {
        return Color.HSBtoRGB(this.hueSlider.getSliderState(), percent, 1.0f);
    }

    public void updateSaturation(float percent) {
        this.pushColorUpdate();
    }

    public int renderBrightnessForeground(float percent) {
        return Color.HSBtoRGB(this.hueSlider.getSliderState(), this.saturationSlider.getSliderState(), percent);
    }

    public void updateBrightness(float percent) {
        this.pushColorUpdate();
    }

    public void pushColorUpdate() {
        float hue = this.hueSlider.getSliderState();
        float saturation = this.saturationSlider.getSliderState();
        float brightness = this.brightnessSlider.getSliderState();

        this.parentScreen.getMenu().setPaletteColor(Color.HSBtoRGB(hue, saturation, brightness));
    }
}
