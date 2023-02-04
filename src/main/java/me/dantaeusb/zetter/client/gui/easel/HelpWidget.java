package me.dantaeusb.zetter.client.gui.easel;

import com.mojang.blaze3d.systems.RenderSystem;
import me.dantaeusb.zetter.client.gui.EaselScreen;
import me.dantaeusb.zetter.core.ClientHelper;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.IRenderable;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

import javax.annotation.Nullable;

public class HelpWidget extends AbstractPaintingWidget implements IRenderable {
    final static String MANUAL_PAGE = "https://zetter.gallery/wiki/zetter#painting";

    final static int BUTTON_WIDTH = 11;
    final static int BUTTON_HEIGHT = 11;

    final static int BUTTON_POSITION_U = 98;
    final static int BUTTON_POSITION_V = 0;

    boolean clicked = false;

    public HelpWidget(EaselScreen parentScreen, int x, int y) {
        super(parentScreen, x, y, BUTTON_WIDTH, BUTTON_HEIGHT, new TranslationTextComponent("container.zetter.painting.help"));

        if (!ClientHelper.openUriAllowed()) {
            this.active = false;
            this.visible = false;
        }
    }

    @Override
    public @Nullable ITextComponent getTooltip(int mouseX, int mouseY) {
        return new TranslationTextComponent("container.zetter.painting.help");
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.active && this.visible) {
            int iMouseX = (int) mouseX;
            int iMouseY = (int) mouseY;

            if (EaselScreen.isInRect(this.x, this.y, BUTTON_WIDTH, BUTTON_HEIGHT, iMouseX, iMouseY)) {
                this.clicked = true;
                ClientHelper.openUriPrompt(this.parentScreen, MANUAL_PAGE);

                return super.mouseClicked(mouseX, mouseY, button);
            }
        }

        this.clicked = false;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.parentScreen.getMinecraft().getTextureManager().bind(AbstractPaintingWidget.PAINTING_WIDGETS_RESOURCE);

        if (!this.visible) {
            return;
        }

        drawButton(matrixStack, mouseX, mouseY);
    }

    protected void drawButton(MatrixStack matrixStack, int mouseX, int mouseY) {
        int buttonU = BUTTON_POSITION_U;

        if (this.clicked) {
            buttonU += BUTTON_WIDTH * 2;
        } else if (EaselScreen.isInRect(this.x, this.y, BUTTON_WIDTH, BUTTON_HEIGHT, mouseX, mouseY)) {
            buttonU += BUTTON_WIDTH;
        }

        this.blit(matrixStack, this.x, this.y, buttonU, BUTTON_POSITION_V, BUTTON_WIDTH, BUTTON_HEIGHT);
    }
}
