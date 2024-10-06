package me.dantaeusb.zetter.client.gui.painting.colorpicker;

import me.dantaeusb.zetter.client.gui.PaintingScreen;
import me.dantaeusb.zetter.client.gui.painting.AbstractPaintingWidget;
import me.dantaeusb.zetter.menu.EaselMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;

public class PaletteWidget extends AbstractPaintingWidget implements Renderable {
    final static int PALETTE_CELL_SIZE = 10;
    final static int PALETTE_OFFSET = PALETTE_CELL_SIZE + 1; // 1px border between slots

    final static int PALETTE_COLS = 2;
    final static int HEIGHT = PALETTE_OFFSET * PALETTE_COLS + 1;
    final static int WIDTH = ((EaselMenu.PALETTE_SLOTS / PALETTE_COLS) * PALETTE_OFFSET) + 1;

    public static final int SWAP_HOTKEY = GLFW.GLFW_KEY_X;

    private final Orientation orientation;

    public PaletteWidget(PaintingScreen parentScreen, int x, int y, Orientation orientation) {
        super(
            parentScreen, x, y,
            orientation == Orientation.HORIZONTAL ? WIDTH : HEIGHT,
            orientation == Orientation.HORIZONTAL ? HEIGHT : WIDTH,
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

        for (int i = 0; i < EaselMenu.PALETTE_SLOTS; i++) {
            int slotX = this.getX() + (i % 2) * PALETTE_OFFSET;
            int slotY = this.getY() + (i / 2) * PALETTE_OFFSET;

            if (isInRect(slotX, slotY, PALETTE_CELL_SIZE, PALETTE_CELL_SIZE, iMouseX, iMouseY) && this.isValidClickButton(button)) {
                slotIndex = i;
                break;
            }
        }

        // Should only happen if clicked on border
        if (slotIndex == -1) {
            return false;
        }

        this.parentScreen.setPaintingScreenState(
            this.parentScreen.getPaintingScreenState().withCurrentPaletteSlot(slotIndex));

        return true;
    }

    protected void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.drawPalette(guiGraphics);
        this.drawPaletteSelector(guiGraphics, this.parentScreen.getPaintingScreenState().currentPaletteSlot());
    }

    protected void drawPalette(GuiGraphics guiGraphics) {
        final int PALETTE_HORIZONTAL_U = 136;
        final int PALETTE_HORIZONTAL_V = 89;
        final int PALETTE_VERTICAL_U = 120;
        final int PALETTE_VERTICAL_V = 0;

        if (this.orientation == Orientation.HORIZONTAL) {
            guiGraphics.blit(PAINTING_WIDGETS_TEXTURE_RESOURCE, this.getX(), this.getY(), PALETTE_HORIZONTAL_U, PALETTE_HORIZONTAL_V, WIDTH, HEIGHT);

            for (int i = 0; i < EaselMenu.PALETTE_SLOTS; i++) {
                int fromX = this.getX() + 1 + (i / 2) * PALETTE_OFFSET;
                int fromY = this.getY() + 1 + (i % 2) * PALETTE_OFFSET;

                int color = this.parentScreen.getPaletteColor(i).getARGB();

                guiGraphics.fill(fromX, fromY, fromX + PALETTE_CELL_SIZE, fromY + PALETTE_CELL_SIZE, color);
            }
        } else {
            guiGraphics.blit(PAINTING_WIDGETS_TEXTURE_RESOURCE, this.getX(), this.getY(), PALETTE_VERTICAL_U, PALETTE_VERTICAL_V, HEIGHT, WIDTH);

            for (int i = 0; i < EaselMenu.PALETTE_SLOTS; i++) {
                int fromX = this.getX() + 1 + (i % 2) * PALETTE_OFFSET;
                int fromY = this.getY() + 1 + (i / 2) * PALETTE_OFFSET;

                int color = this.parentScreen.getPaletteColor(i).getARGB();

                guiGraphics.fill(fromX, fromY, fromX + PALETTE_CELL_SIZE, fromY + PALETTE_CELL_SIZE, color);
            }
        }
    }

    protected void drawPaletteSelector(GuiGraphics guiGraphics, int currentPaletteSlot) {
        final int SELECTOR_POSITION_U = 90;
        final int SELECTOR_POSITION_V = 164;

        final int PALETTE_BORDER = 3;

        int selectorPositionX = this.getX() + 1 + (currentPaletteSlot % 2 != 0 ? PALETTE_OFFSET : 0) - PALETTE_BORDER;
        int selectorPositionY = this.getY() + 1 + (currentPaletteSlot / 2) * PALETTE_OFFSET - PALETTE_BORDER;

        guiGraphics.blit(PAINTING_WIDGETS_TEXTURE_RESOURCE,  selectorPositionX, selectorPositionY, SELECTOR_POSITION_U, SELECTOR_POSITION_V, PALETTE_CELL_SIZE + PALETTE_BORDER * 2, PALETTE_CELL_SIZE + PALETTE_BORDER * 2);
    }

    public enum Orientation {
        HORIZONTAL,
        VERTICAL
    }
}
