package me.dantaeusb.zetter.client.gui.painting;

import com.mojang.blaze3d.systems.RenderSystem;
import me.dantaeusb.zetter.client.gui.PaintingScreen;
import me.dantaeusb.zetter.menu.EaselContainerMenu;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.Widget;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;

import javax.annotation.Nullable;

public class PaletteWidget extends AbstractPaintingWidget implements Widget {
    final static int PALETTE_SCALE_FACTOR = 10;
    final static int PALETTE_OFFSET = PALETTE_SCALE_FACTOR + 1; // 1px border between slots

    final static int PALETTE_COLS = 2;

    final static int WIDTH = PALETTE_SCALE_FACTOR + PALETTE_OFFSET;
    final static int HEIGHT = PALETTE_SCALE_FACTOR + ((EaselContainerMenu.PALETTE_SLOTS / PALETTE_COLS) * PALETTE_OFFSET);

    public PaletteWidget(PaintingScreen parentScreen, int x, int y) {
        super(parentScreen, x, y, WIDTH, HEIGHT, new TranslatableComponent("container.zetter.painting.palette"));
    }

    @Override
    public @Nullable
    Component getTooltip(int mouseX, int mouseY) {
        return null;
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

        for (int i = 0; i < EaselContainerMenu.PALETTE_SLOTS; i++) {
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

        this.parentScreen.getMenu().setCurrentPaletteSlot(slotIndex);

        return true;
    }

    public void render(PoseStack matrixStack) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, PaintingScreen.PAINTING_RESOURCE);

        drawPalette(matrixStack);
        drawPaletteSelector(matrixStack, this.parentScreen.getMenu().getCurrentPaletteSlot());
    }

    protected void drawPalette(PoseStack matrixStack) {
        if (!this.parentScreen.isPaletteAvailable()) {
            return;
        }

        for (int i = 0; i < EaselContainerMenu.PALETTE_SLOTS; i++) {
            int fromX = this.x + (i % 2) * PALETTE_OFFSET;
            int fromY = this.y + (i / 2) * PALETTE_OFFSET;

            int color = this.parentScreen.getMenu().getPaletteColor(i);

            fill(matrixStack, fromX, fromY, fromX + PALETTE_SCALE_FACTOR, fromY + PALETTE_SCALE_FACTOR, color);
        }
    }

    protected void drawPaletteSelector(PoseStack matrixStack, int currentPaletteSlot) {
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
}
