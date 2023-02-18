package me.dantaeusb.zetter.client.gui.easel;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.matrix.MatrixStack;
import me.dantaeusb.zetter.client.gui.EaselScreen;
import me.dantaeusb.zetter.core.tools.Color;
import me.dantaeusb.zetter.painting.parameters.AbstractToolParameters;
import me.dantaeusb.zetter.painting.parameters.DitheringParameterHolder;
import me.dantaeusb.zetter.painting.pipes.DitheringPipe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.IRenderable;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class DitheringWidget extends AbstractPaintingWidget implements IRenderable {
    private final static int WIDTH = 80;
    private final static int HEIGHT = 32;
    private final static int FONT_Y_MARGIN = 12;

    private final static int DITHERING_BUTTON_WIDTH = 20;

    private final static int DITHERING_BUTTON_HEIGHT = 20;

    private final List<DitheringButton> buttons;

    public DitheringWidget(EaselScreen parentScreen, int x, int y) {
        super(parentScreen, x, y, WIDTH, HEIGHT, new TranslationTextComponent("container.zetter.painting.dithering"));

        final int DITHERING_BUTTON_U = 0;
        final int DITHERING_BUTTON_V = 32;

        this.buttons = new ArrayList<DitheringButton>() {{
            add(new DitheringButton(DitheringPipe.DitheringOption.NO_DITHERING, DITHERING_BUTTON_U, DITHERING_BUTTON_V, DITHERING_BUTTON_WIDTH, DITHERING_BUTTON_HEIGHT));
            add(new DitheringButton(DitheringPipe.DitheringOption.DENSE_DITHERING, DITHERING_BUTTON_U + DITHERING_BUTTON_WIDTH, DITHERING_BUTTON_V, DITHERING_BUTTON_WIDTH, DITHERING_BUTTON_HEIGHT));
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
        for (DitheringButton ditheringButton: this.buttons) {
            int fromX = this.x + i * DITHERING_BUTTON_WIDTH;

            if (EaselScreen.isInRect(fromX, this.y + FONT_Y_MARGIN, ditheringButton.width, ditheringButton.height, iMouseX, iMouseY) && this.isValidClickButton(button)) {
                AbstractToolParameters parameters = this.parentScreen.getMenu().getCurrentToolParameters();

                if (parameters instanceof DitheringParameterHolder) {
                    ((DitheringParameterHolder) parameters).setDithering(ditheringButton.dithering);
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
        DitheringPipe.DitheringOption dithering = null;

        if (parameters instanceof DitheringParameterHolder) {
            dithering = ((DitheringParameterHolder) parameters).getDithering();
        } else {
            throw new RuntimeException("Cannot render dithering parameter");
        }

        int i = 0;
        for (DitheringButton ditheringButton: this.buttons) {
            int fromX = this.x + i * DITHERING_BUTTON_WIDTH;
            int vOffset = dithering == ditheringButton.dithering ? DITHERING_BUTTON_HEIGHT : 0;

            this.blit(matrixStack, fromX, this.y + FONT_Y_MARGIN, ditheringButton.uPosition, ditheringButton.vPosition + vOffset, ditheringButton.width, ditheringButton.height);
            i++;
        }
    }

    public void renderLabels(MatrixStack matrixStack, int mouseX, int mouseY) {
        this.parentScreen.getFont().draw(matrixStack, this.getMessage(), (float) this.x - this.parentScreen.getGuiLeft(), (float) this.y - this.parentScreen.getGuiTop(), Color.DARK_GRAY.getRGB());
    }

    public class DitheringButton {
        private final DitheringPipe.DitheringOption dithering;
        public final int uPosition;
        public final int vPosition;
        public final int height;
        public final int width;

        DitheringButton(DitheringPipe.DitheringOption dithering, int uPosition, int vPosition, int width, int height) {
            this.dithering = dithering;
            this.uPosition = uPosition;
            this.vPosition = vPosition;
            this.height = height;
            this.width = width;
        }

        public ITextComponent getTooltip() {
            return this.dithering.translatableComponent;
        }
    }
}
