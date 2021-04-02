package com.dantaeusb.zetter.client.gui.painting;

import com.dantaeusb.zetter.Zetter;
import com.dantaeusb.zetter.client.gui.PaintingScreen;
import com.dantaeusb.zetter.container.EaselContainer;
import com.dantaeusb.zetter.item.CanvasItem;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.IRenderable;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

public class PaletteWidget extends AbstractPaintingWidget implements IRenderable, IGuiEventListener {
    final static int PALETTE_SCALE_FACTOR = 10;
    final static int PALETTE_OFFSET = PALETTE_SCALE_FACTOR + 1; // 1px border between slots

    final static int PALETTE_COLS = 2;

    final static int WIDTH = PALETTE_SCALE_FACTOR + PALETTE_OFFSET;
    final static int HEIGHT = PALETTE_SCALE_FACTOR + ((EaselContainer.PALETTE_SLOTS / PALETTE_COLS) * PALETTE_OFFSET);

    private int currentPaletteSlot = 0;

    public PaletteWidget(PaintingScreen parentScreen, int x, int y) {
        super(parentScreen, x, y, WIDTH, HEIGHT, new TranslationTextComponent("container.zetter.painting.palette"));
    }

    public int getCurrentPaletteSlot() {
        return this.currentPaletteSlot;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int iMouseX = (int) mouseX;
        int iMouseY = (int) mouseY;

        int slotIndex = -1;

        // Quick check
        if (!this.isMouseOver(mouseX, mouseY)) {
            return false;
        }

        for (int i = 0; i < EaselContainer.PALETTE_SLOTS; i++) {
            int slotX = this.x + (i % 2) * PALETTE_OFFSET;
            int slotY = this.y + (i / 2) * PALETTE_OFFSET;

            if (PaintingScreen.isInRect(slotX, slotY, PALETTE_SCALE_FACTOR, PALETTE_SCALE_FACTOR, iMouseX, iMouseY) && this.isValidClickButton(button)) {
                slotIndex = i;
                break;
            }
        }

        // Should only happen if clicked on border
        if (slotIndex == -1) {
            return false;
        }

        this.setCurrentPaletteSlot(slotIndex);

        return true;
    }

    public void render(MatrixStack matrixStack) {
        drawPalette(matrixStack);
        drawPaletteSelector(matrixStack);
    }

    protected void drawPalette(MatrixStack matrixStack) {
        if (!this.parentScreen.isPaletteAvailable()) {
            return;
        }

        for (int i = 0; i < EaselContainer.PALETTE_SLOTS; i++) {
            int fromX = this.x + (i % 2) * PALETTE_OFFSET;
            int fromY = this.y + (i / 2) * PALETTE_OFFSET;

            int color = this.parentScreen.getPaletteColor(i);

            this.fillGradient(matrixStack, fromX, fromY, fromX + PALETTE_SCALE_FACTOR, fromY + PALETTE_SCALE_FACTOR, color, color);
        }
    }

    protected void drawPaletteSelector(MatrixStack matrixStack) {
        if (!this.parentScreen.isPaletteAvailable()) {
            return;
        }

        final int SELECTOR_POSITION_U = 82;
        final int SELECTOR_POSITION_V = 185;

        final int PALETTE_BORDER = 3;

        int selectorPositionX = this.x + (currentPaletteSlot % 2 != 0 ? PALETTE_OFFSET : 0) - PALETTE_BORDER;
        int selectorPositionY = this.y + (currentPaletteSlot / 2) * PALETTE_OFFSET - PALETTE_BORDER;

        this.blit(matrixStack, selectorPositionX, selectorPositionY, SELECTOR_POSITION_U, SELECTOR_POSITION_V, PALETTE_SCALE_FACTOR + PALETTE_BORDER * 2, PALETTE_SCALE_FACTOR + PALETTE_BORDER * 2);
    }

    protected void setCurrentPaletteSlot(int slotIndex) {
        this.currentPaletteSlot = slotIndex;

        this.parentScreen.updateSlidersWithCurrentColor();
    }
}
