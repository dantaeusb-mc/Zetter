package com.dantaeusb.zetter.client.gui.painting;

import com.dantaeusb.zetter.client.gui.PaintingScreen;
import com.dantaeusb.zetter.core.Helper;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.IRenderable;
import net.minecraft.util.Util;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

import javax.annotation.Nullable;

public class HelpWidget extends AbstractPaintingWidget implements IRenderable {
    final static String MANUAL_PAGE = "https://zetter.gallery/wiki/zetter#painting";

    final static int BUTTON_WIDTH = 11;
    final static int BUTTON_HEIGHT = 11;

    final static int BUTTON_POSITION_U = 176;
    final static int BUTTON_POSITION_V = 174;

    boolean clicked = false;

    public HelpWidget(PaintingScreen parentScreen, int x, int y) {
        super(parentScreen, x, y, BUTTON_WIDTH, BUTTON_HEIGHT, new TranslationTextComponent("container.zetter.painting.help"));

        if (!Helper.openUriAllowed()) {
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

            if (PaintingScreen.isInRect(this.x, this.y, BUTTON_WIDTH, BUTTON_HEIGHT, iMouseX, iMouseY)) {
                this.clicked = true;
                Helper.openUriPrompt(this.parentScreen, MANUAL_PAGE);

                return super.mouseClicked(mouseX, mouseY, button);
            }
        }

        this.clicked = false;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        if (!this.visible) {
            return;
        }

        drawButton(matrixStack, mouseX, mouseY);
    }

    protected void drawButton(MatrixStack matrixStack, int mouseX, int mouseY) {
        int buttonU = BUTTON_POSITION_U;

        if (this.clicked) {
            buttonU += BUTTON_WIDTH * 2;
        } else if (PaintingScreen.isInRect(this.x, this.y, BUTTON_WIDTH, BUTTON_HEIGHT, mouseX, mouseY)) {
            buttonU += BUTTON_WIDTH;
        }

        this.blit(matrixStack, this.x, this.y, buttonU, BUTTON_POSITION_V, BUTTON_WIDTH, BUTTON_HEIGHT);
    }
}
