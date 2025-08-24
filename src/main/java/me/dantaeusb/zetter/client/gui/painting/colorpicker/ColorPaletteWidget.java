package me.dantaeusb.zetter.client.gui.painting.colorpicker;

import me.dantaeusb.zetter.client.gui.PaintingScreen;
import me.dantaeusb.zetter.client.gui.painting.AbstractPaintingWidget;
import me.dantaeusb.zetter.item.PaletteItem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public class ColorPaletteWidget extends AbstractPaintingWidget implements Renderable {
    final static int PALETTE_CELL_SIZE = 10;
    final static int PALETTE_OFFSET = PALETTE_CELL_SIZE + 1; // 1px border between slots

    final static int PALETTE_COLS = 2;
    final static int PALETTE_LENGTH = ((PaletteItem.PALETTE_SIZE / PALETTE_COLS) * PALETTE_OFFSET) + 1;
    final static int PALETTE_WIDTH = PALETTE_OFFSET * PALETTE_COLS + 1;

    private final Orientation orientation;

    public ColorPaletteWidget(PaintingScreen parentScreen, int x, int y, Orientation orientation) {
        super(
            parentScreen, x, y,
            orientation == Orientation.HORIZONTAL ? PALETTE_LENGTH : PALETTE_WIDTH,
            orientation == Orientation.HORIZONTAL ? PALETTE_WIDTH : PALETTE_LENGTH,
            Component.translatable("container.zetter.painting.palette")
        );

        this.orientation = orientation;
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

        if (this.orientation == Orientation.HORIZONTAL) {
            for (int i = 0; i < PaletteItem.PALETTE_SIZE; i++) {
                int slotX = this.getX() + (i / 2) * PALETTE_OFFSET;
                int slotY = this.getY() + (i % 2) * PALETTE_OFFSET;

                if (isInRect(slotX, slotY, PALETTE_CELL_SIZE, PALETTE_CELL_SIZE, iMouseX, iMouseY) && this.isValidClickButton(button)) {
                    slotIndex = i;
                    break;
                }
            }
        } else {
            for (int i = 0; i < PaletteItem.PALETTE_SIZE; i++) {
                int slotX = this.getX() + (i % 2) * PALETTE_OFFSET;
                int slotY = this.getY() + (i / 2) * PALETTE_OFFSET;

                if (isInRect(slotX, slotY, PALETTE_CELL_SIZE, PALETTE_CELL_SIZE, iMouseX, iMouseY) && this.isValidClickButton(button)) {
                    slotIndex = i;
                    break;
                }
            }
        }

        // Should only happen if clicked on border
        if (slotIndex == -1) {
            return false;
        }

        this.parentScreen.setPaletteState(
            this.parentScreen.getPaletteState().withCurrentPaletteSlot(slotIndex)
        );

        return true;
    }

    protected void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.drawPalette(guiGraphics);
        this.drawPaletteSelector(guiGraphics);
    }

    protected void drawPalette(GuiGraphics guiGraphics) {
        final int PALETTE_HORIZONTAL_U = 136;
        final int PALETTE_HORIZONTAL_V = 89;
        final int PALETTE_VERTICAL_U = 120;
        final int PALETTE_VERTICAL_V = 0;

        if (this.orientation == Orientation.HORIZONTAL) {
            guiGraphics.blit(PAINTING_WIDGETS_TEXTURE_RESOURCE, this.getX(), this.getY(), PALETTE_HORIZONTAL_U, PALETTE_HORIZONTAL_V, PALETTE_LENGTH, PALETTE_WIDTH);

            for (int i = 0; i < PaletteItem.PALETTE_SIZE; i++) {
                int fromX = this.getX() + 1 + (i / 2) * PALETTE_OFFSET;
                int fromY = this.getY() + 1 + (i % 2) * PALETTE_OFFSET;

                int color = this.parentScreen.getPaletteColor(i).getARGB();

                guiGraphics.fill(fromX, fromY, fromX + PALETTE_CELL_SIZE, fromY + PALETTE_CELL_SIZE, color);
            }
        } else {
            guiGraphics.blit(PAINTING_WIDGETS_TEXTURE_RESOURCE, this.getX(), this.getY(), PALETTE_VERTICAL_U, PALETTE_VERTICAL_V, PALETTE_WIDTH, PALETTE_LENGTH);

            for (int i = 0; i < PaletteItem.PALETTE_SIZE; i++) {
                int fromX = this.getX() + 1 + (i % 2) * PALETTE_OFFSET;
                int fromY = this.getY() + 1 + (i / 2) * PALETTE_OFFSET;

                int color = this.parentScreen.getPaletteColor(i).getARGB();

                guiGraphics.fill(fromX, fromY, fromX + PALETTE_CELL_SIZE, fromY + PALETTE_CELL_SIZE, color);
            }
        }
    }

    protected void drawPaletteSelector(GuiGraphics guiGraphics) {
        final int SELECTOR_POSITION_U = 90;
        final int SELECTOR_POSITION_V = 164;

        final int PALETTE_BORDER = 3;

        final int currentPaletteSlot = this.parentScreen.getPaletteState().currentPaletteSlot();

        if (this.orientation == Orientation.VERTICAL) {
            int selectorPositionX = this.getX() + 1 + (currentPaletteSlot % 2 != 0 ? PALETTE_OFFSET : 0) - PALETTE_BORDER;
            int selectorPositionY = this.getY() + 1 + (currentPaletteSlot / 2) * PALETTE_OFFSET - PALETTE_BORDER;

            guiGraphics.blit(PAINTING_WIDGETS_TEXTURE_RESOURCE,  selectorPositionX, selectorPositionY, SELECTOR_POSITION_U, SELECTOR_POSITION_V, PALETTE_CELL_SIZE + PALETTE_BORDER * 2, PALETTE_CELL_SIZE + PALETTE_BORDER * 2);
        } else {
            int selectorPositionX = this.getX() + 1 + (currentPaletteSlot / 2) * PALETTE_OFFSET - PALETTE_BORDER;
            int selectorPositionY = this.getY() + 1 + (currentPaletteSlot % 2 != 0 ? PALETTE_OFFSET : 0) - PALETTE_BORDER;

            guiGraphics.blit(PAINTING_WIDGETS_TEXTURE_RESOURCE,  selectorPositionX, selectorPositionY, SELECTOR_POSITION_U, SELECTOR_POSITION_V, PALETTE_CELL_SIZE + PALETTE_BORDER * 2, PALETTE_CELL_SIZE + PALETTE_BORDER * 2);
        }
    }

    public enum Orientation {
        HORIZONTAL,
        VERTICAL
    }
}
