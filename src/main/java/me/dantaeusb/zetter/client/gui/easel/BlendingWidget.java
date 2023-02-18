package me.dantaeusb.zetter.client.gui.easel;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.matrix.MatrixStack;
import me.dantaeusb.zetter.client.gui.EaselScreen;
import me.dantaeusb.zetter.core.tools.Color;
import me.dantaeusb.zetter.painting.parameters.AbstractToolParameters;
import me.dantaeusb.zetter.painting.parameters.BlendingParameterHolder;
import me.dantaeusb.zetter.painting.pipes.BlendingPipe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.IRenderable;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class BlendingWidget extends AbstractPaintingWidget implements IRenderable {
    public final static int WIDTH = 60;
    public final static int HEIGHT = 32;
    public final static int FONT_Y_MARGIN = 12;

    private final static int BLENDING_BUTTON_WIDTH = 20;

    private final static int BLENDING_BUTTON_HEIGHT = 20;

    private final List<BlendingButton> buttons;

    public BlendingWidget(EaselScreen parentScreen, int x, int y) {
        super(parentScreen, x, y, WIDTH, HEIGHT, new TranslationTextComponent("container.zetter.painting.blending"));

        final int BLENDING_BUTTON_U = 80;
        final int BLENDING_BUTTON_V = 32;

        this.buttons = new ArrayList<BlendingButton>() {{
            add(new BlendingButton(BlendingPipe.BlendingOption.RYB, BLENDING_BUTTON_U, BLENDING_BUTTON_V, BLENDING_BUTTON_WIDTH, BLENDING_BUTTON_HEIGHT));
            add(new BlendingButton(BlendingPipe.BlendingOption.RGB, BLENDING_BUTTON_U + BLENDING_BUTTON_WIDTH, BLENDING_BUTTON_V, BLENDING_BUTTON_WIDTH, BLENDING_BUTTON_HEIGHT));
            add(new BlendingButton(BlendingPipe.BlendingOption.RGBC, BLENDING_BUTTON_U + BLENDING_BUTTON_WIDTH * 2, BLENDING_BUTTON_V, BLENDING_BUTTON_WIDTH, BLENDING_BUTTON_HEIGHT));
        }};
    }

    @Override
    public @Nullable ITextComponent getTooltip(int mouseX, int mouseY) {
        return null;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int iMouseX = (int) mouseX;
        int iMouseY = (int) mouseY;

        // Quick check
        if (!this.isMouseOver(mouseX, mouseY)) {
            return false;
        }

        int i = 0;
        for (BlendingButton blendingButton: this.buttons) {
            int fromX = this.x + i * BLENDING_BUTTON_WIDTH;

            if (EaselScreen.isInRect(fromX, this.y + FONT_Y_MARGIN, blendingButton.width, blendingButton.height, iMouseX, iMouseY) && this.isValidClickButton(button)) {
                AbstractToolParameters parameters = this.parentScreen.getMenu().getCurrentToolParameters();

                if (parameters instanceof BlendingParameterHolder) {
                    ((BlendingParameterHolder) parameters).setBlending(blendingButton.blending);
                } else {
                    throw new RuntimeException("Cannot apply blending parameter");
                }

                this.playDownSound(Minecraft.getInstance().getSoundManager());
                return true;
            }

            i++;
        }

        return false;
    }

    public void render(MatrixStack matrixStack) {
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.parentScreen.getMinecraft().getTextureManager().bind(AbstractPaintingWidget.PAINTING_WIDGETS_RESOURCE);

        AbstractToolParameters parameters = this.parentScreen.getMenu().getCurrentToolParameters();
        BlendingPipe.BlendingOption blending = null;

        if (parameters instanceof BlendingParameterHolder) {
            blending = ((BlendingParameterHolder) parameters).getBlending();
        } else {
            throw new RuntimeException("Cannot render blending parameter");
        }

        int i = 0;
        for (BlendingButton blendingButton: this.buttons) {
            int fromX = this.x + i * BLENDING_BUTTON_WIDTH;
            int vOffset = blending == blendingButton.blending ? BLENDING_BUTTON_HEIGHT : 0;

            this.blit(matrixStack, fromX, this.y + FONT_Y_MARGIN, blendingButton.uPosition, blendingButton.vPosition + vOffset, blendingButton.width, blendingButton.height);
            i++;
        }
    }

    public void renderLabels(MatrixStack matrixStack, int mouseX, int mouseY) {
        this.parentScreen.getFont().draw(matrixStack, this.getMessage(), (float) this.x - this.parentScreen.getGuiLeft(), (float) this.y - this.parentScreen.getGuiTop(), Color.DARK_GRAY.getRGB());
    }

    public class BlendingButton {
        private final BlendingPipe.BlendingOption blending;
        public final int uPosition;
        public final int vPosition;
        public final int height;
        public final int width;

        BlendingButton(BlendingPipe.BlendingOption blending, int uPosition, int vPosition, int width, int height) {
            this.blending = blending;
            this.uPosition = uPosition;
            this.vPosition = vPosition;
            this.height = height;
            this.width = width;
        }

        public ITextComponent getTooltip() {
            return this.blending.translatableComponent;
        }
    }
}
