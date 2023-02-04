package me.dantaeusb.zetter.client.gui.easel;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.matrix.MatrixStack;
import me.dantaeusb.zetter.client.gui.EaselScreen;
import net.minecraft.client.gui.IRenderable;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;

import javax.annotation.Nullable;
import java.util.function.Consumer;

public class SliderWidget extends AbstractPaintingWidget implements IRenderable {
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
            EaselScreen parentScreen, int x, int y, ITextComponent translatableComponent,
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
    public @Nullable ITextComponent getTooltip(int mouseX, int mouseY) {
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

    public void render(MatrixStack matrixStack) {
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.parentScreen.getMinecraft().getTextureManager().bind(AbstractPaintingWidget.PAINTING_WIDGETS_RESOURCE);

        this.drawSliderBackground(matrixStack);
        this.drawSliderForeground(matrixStack);
        this.drawHandler(matrixStack);
    }

    protected void drawSliderBackground(MatrixStack matrixStack) {
        final int SLIDER_POSITION_U = 5;
        final int SLIDER_POSITION_V = 80;

        int sliderV = SLIDER_POSITION_V;

        if (this.sliderDragging) {
            sliderV += HEIGHT;
        }

        this.blit(matrixStack, this.x, this.y, SLIDER_POSITION_U, sliderV, WIDTH, HEIGHT);
    }

    protected void drawSliderForeground(MatrixStack matrixStack) {
        int sliderContentGlobalLeft = this.x + 3;
        int sliderContentGlobalTop = this.y + 3;

        int sliderContentWidth = WIDTH - 6;
        int sliderContentHeight = 3;

        if (this.sliderDragging) {
            sliderContentGlobalTop -= 2;
            sliderContentHeight += 4;

            if (this.backgroundLambda != null) {
                this.backgroundLambda.accept(matrixStack, sliderContentGlobalLeft, sliderContentGlobalTop, sliderContentWidth, sliderContentHeight);
            }
        } else if (this.backgroundLambda != null) {
            this.backgroundLambda.accept(matrixStack, sliderContentGlobalLeft, sliderContentGlobalTop, sliderContentWidth, sliderContentHeight);
        }

        // We're not doing gradient because it's too smooth for Minecraft :)
        for (int i = 0; i < sliderContentWidth; i++) {
            int color = this.foregroundLambda.accept((float) i / sliderContentWidth);

            fill(matrixStack, sliderContentGlobalLeft + i, sliderContentGlobalTop, sliderContentGlobalLeft + i + 1, sliderContentGlobalTop + sliderContentHeight, color);
        }
    }

    /**
     *
     * @param mouseX
     * @param mouseY
     */
    protected void handleSliderInteraction(final int mouseX, final int mouseY) {
        this.sliderDragging = true;

        float percent = (float) (mouseX - this.x - 3) / (WIDTH - 7);
        percent = MathHelper.clamp(percent, 0.0f, 1.0f);

        this.sliderState = percent;

        this.positionConsumer.accept(percent);
    }

    /**
     * Handlers
     */

    protected void drawHandler(MatrixStack matrixStack) {
        final int HANDLER_POSITION_U = 0;
        final int HANDLER_POSITION_V = 79;
        final int HANDLER_WIDTH = 5;
        final int HANDLER_HEIGHT = 11;

        int sliderContentWidth = WIDTH - 7;

        int sliderGlobalLeft = this.x + (int) (sliderContentWidth * this.sliderState) + 3 - 2;
        int sliderGlobalTop = this.y - 1;

        int sliderV = HANDLER_POSITION_V;

        if (this.sliderDragging) {
            sliderV += HANDLER_HEIGHT;
        }

        this.blit(matrixStack, sliderGlobalLeft, sliderGlobalTop, HANDLER_POSITION_U, sliderV, HANDLER_WIDTH, HANDLER_HEIGHT);
    }

    @FunctionalInterface
    public interface BackgroundConsumer {
        public void accept(MatrixStack matrixStack, int x, int y, int width, int height);
    }

    /**
     * Calls for every pixel of the slider, waits for argb integer
     */
    @FunctionalInterface
    public interface ForegroundColorFunction {
        public int accept(float percent);
    }
}
