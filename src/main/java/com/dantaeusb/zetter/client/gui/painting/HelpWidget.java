package com.dantaeusb.zetter.client.gui.painting;

import com.dantaeusb.zetter.client.gui.PaintingScreen;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.IRenderable;
import net.minecraft.util.Util;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

import javax.annotation.Nullable;

public class HelpWidget extends AbstractPaintingWidget implements IRenderable {
    final static String MANUAL_PAGE = "https://zetter.gallery/wiki/zetter#painting";

    final static int BUTTON_WIDTH = 18;
    final static int BUTTON_HEIGHT = 18;

    final static int BUTTON_POSITION_U = 176;
    final static int BUTTON_POSITION_V = 167;

    boolean clicked = false;

    public HelpWidget(PaintingScreen parentScreen, int x, int y) {
        super(parentScreen, x, y, BUTTON_WIDTH, BUTTON_HEIGHT, new TranslationTextComponent("container.zetter.painting.help"));
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
                Util.getOSType().openURI(MANUAL_PAGE);

                return super.mouseClicked(mouseX, mouseY, button);
            }
        }

        this.clicked = false;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
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
