package me.dantaeusb.zetter.client.gui.easel;

import me.dantaeusb.zetter.client.gui.EaselScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.function.Consumer;

public class SliderWidget extends AbstractEaselWidget implements Renderable {
    final static int WIDTH = 150;
    final static int HEIGHT = 9;

    private float sliderState = 0.0f;
    private boolean sliderDragging = false;

    private BackgroundConsumer backgroundLambda;
    private ForegroundColorFunction foregroundLambda;
    private Consumer<Float> positionConsumer;

    /**
     * update value callback
     * color (foreground) fill callback
     */

    public SliderWidget(
            EaselScreen parentScreen, int x, int y, Component translatableComponent,
            Consumer<Float> positionConsumer,
            @Nullable BackgroundConsumer backgroundLambda, @Nullable ForegroundColorFunction foregroundLambda
    ) {
        super(parentScreen, x, y, WIDTH, HEIGHT, translatableComponent);

        this.backgroundLambda = backgroundLambda;
        this.foregroundLambda = foregroundLambda;
        this.positionConsumer = positionConsumer;
    }

    public float getSliderState() {
        return this.sliderState;
    }

    public void setSliderState(float percent) {
        this.sliderState = percent;
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
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.sliderDragging) {
            int iMouseX = (int) mouseX;
            int iMouseY = (int) mouseY;

            this.handleSliderInteraction(iMouseX, iMouseY);
            return true;
        }

        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.sliderDragging) {
            // If we were changing palette colors, sync them with server
            // this.parentScreen.getMenu().sendPaletteUpdatePacket();
        }

        this.sliderDragging = false;

        return super.mouseReleased(mouseX, mouseY, button);
    }

    public void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.drawSliderBackground(guiGraphics);
        this.drawSliderForeground(guiGraphics);
        this.drawHandler(guiGraphics);
    }

    protected void drawSliderBackground(GuiGraphics guiGraphics) {
        final int SLIDER_POSITION_U = 5;
        final int SLIDER_POSITION_V = 80;

        int sliderV = SLIDER_POSITION_V;

        if (this.sliderDragging) {
            sliderV += HEIGHT;
        }

        guiGraphics.blit(AbstractEaselWidget.EASEL_WIDGETS_TEXTURE_RESOURCE,  this.getX(), this.getY(), SLIDER_POSITION_U, sliderV, WIDTH, HEIGHT);
    }

    protected void drawSliderForeground(GuiGraphics guiGraphics) {
        int sliderContentGlobalLeft = this.getX() + 3;
        int sliderContentGlobalTop = this.getY() + 3;

        int sliderContentWidth = WIDTH - 6;
        int sliderContentHeight = 3;

        if (this.sliderDragging) {
            sliderContentGlobalTop -= 2;
            sliderContentHeight += 4;

            if (this.backgroundLambda != null) {
                this.backgroundLambda.accept(guiGraphics, sliderContentGlobalLeft, sliderContentGlobalTop, sliderContentWidth, sliderContentHeight);
            }
        } else if (this.backgroundLambda != null) {
            this.backgroundLambda.accept(guiGraphics, sliderContentGlobalLeft, sliderContentGlobalTop, sliderContentWidth, sliderContentHeight);
        }

        // We're not doing gradient because it's too smooth for Minecraft :)
        for (int i = 0; i < sliderContentWidth; i++) {
            int color = this.foregroundLambda.accept((float) i / sliderContentWidth);

            guiGraphics.fill(sliderContentGlobalLeft + i, sliderContentGlobalTop, sliderContentGlobalLeft + i + 1, sliderContentGlobalTop + sliderContentHeight, color);
        }
    }

    /**
     *
     * @param mouseX
     * @param mouseY
     */
    protected void handleSliderInteraction(final int mouseX, final int mouseY) {
        this.sliderDragging = true;

        float percent = (float) (mouseX - this.getX() - 3) / (WIDTH - 7);
        percent = Mth.clamp(percent, 0.0f, 1.0f);

        this.sliderState = percent;

        this.positionConsumer.accept(percent);
    }

    /**
     * Handlers
     */

    protected void drawHandler(GuiGraphics guiGraphics) {
        final int HANDLER_POSITION_U = 0;
        final int HANDLER_POSITION_V = 79;
        final int HANDLER_WIDTH = 5;
        final int HANDLER_HEIGHT = 11;

        int sliderContentWidth = WIDTH - 7;

        int sliderGlobalLeft = this.getX() + (int) (sliderContentWidth * this.sliderState) + 3 - 2;
        int sliderGlobalTop = this.getY() - 1;

        int sliderV = HANDLER_POSITION_V;

        if (this.sliderDragging) {
            sliderV += HANDLER_HEIGHT;
        }

        guiGraphics.blit(AbstractEaselWidget.EASEL_WIDGETS_TEXTURE_RESOURCE,  sliderGlobalLeft, sliderGlobalTop, HANDLER_POSITION_U, sliderV, HANDLER_WIDTH, HANDLER_HEIGHT);
    }

    @FunctionalInterface
    public interface BackgroundConsumer {
        public void accept(GuiGraphics guiGraphics, int x, int y, int width, int height);
    }

    /**
     * Calls for every pixel of the slider, waits for argb integer
     */
    @FunctionalInterface
    public interface ForegroundColorFunction {
        public int accept(float percent);
    }
}
