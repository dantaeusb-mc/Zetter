package me.dantaeusb.zetter.client.gui.easel;

import com.mojang.blaze3d.systems.RenderSystem;
import me.dantaeusb.zetter.client.gui.EaselScreen;
import me.dantaeusb.zetter.menu.EaselMenu;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.IRenderable;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;

public class PaletteWidget extends AbstractPaintingWidget implements IRenderable {
    final static int PALETTE_SCALE_FACTOR = 10;
    final static int PALETTE_OFFSET = PALETTE_SCALE_FACTOR + 1; // 1px border between slots

    final static int PALETTE_COLS = 2;
    final static int WIDTH = PALETTE_SCALE_FACTOR + PALETTE_OFFSET;
    final static int HEIGHT = PALETTE_SCALE_FACTOR + ((EaselMenu.PALETTE_SLOTS / PALETTE_COLS) * PALETTE_OFFSET);

    public static final int SWAP_HOTKEY = GLFW.GLFW_KEY_X;

    public PaletteWidget(EaselScreen parentScreen, int x, int y) {
        super(parentScreen, x, y, WIDTH, HEIGHT, new TranslationTextComponent("container.zetter.painting.palette"));
    }

    @Override
    public @Nullable
    ITextComponent getTooltip(int mouseX, int mouseY) {
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
            int slotX = this.x + (i % 2) * PALETTE_OFFSET;
            int slotY = this.y + (i / 2) * PALETTE_OFFSET;

            if (EaselScreen.isInRect(slotX, slotY, PALETTE_SCALE_FACTOR, PALETTE_SCALE_FACTOR, iMouseX, iMouseY) && this.isValidClickButton(button)) {
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

    public void render(MatrixStack matrixStack) {
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.parentScreen.getMinecraft().getTextureManager().bind(AbstractPaintingWidget.PAINTING_WIDGETS_RESOURCE);

        drawPalette(matrixStack);
        drawPaletteSelector(matrixStack, this.parentScreen.getMenu().getCurrentPaletteSlot());
    }

    protected void drawPalette(MatrixStack matrixStack) {
        if (!this.parentScreen.getMenu().isPaletteAvailable()) {
            return;
        }

        for (int i = 0; i < EaselMenu.PALETTE_SLOTS; i++) {
            int fromX = this.x + (i % 2) * PALETTE_OFFSET;
            int fromY = this.y + (i / 2) * PALETTE_OFFSET;

            int color = this.parentScreen.getMenu().getPaletteColor(i);

            fill(matrixStack, fromX, fromY, fromX + PALETTE_SCALE_FACTOR, fromY + PALETTE_SCALE_FACTOR, color);
        }
    }

    protected void drawPaletteSelector(MatrixStack matrixStack, int currentPaletteSlot) {
        if (!this.parentScreen.getMenu().isPaletteAvailable()) {
            return;
        }

        final int SELECTOR_POSITION_U = 82;
        final int SELECTOR_POSITION_V = 0;

        final int PALETTE_BORDER = 3;

        int selectorPositionX = this.x + (currentPaletteSlot % 2 != 0 ? PALETTE_OFFSET : 0) - PALETTE_BORDER;
        int selectorPositionY = this.y + (currentPaletteSlot / 2) * PALETTE_OFFSET - PALETTE_BORDER;

        this.blit(matrixStack, selectorPositionX, selectorPositionY, SELECTOR_POSITION_U, SELECTOR_POSITION_V, PALETTE_SCALE_FACTOR + PALETTE_BORDER * 2, PALETTE_SCALE_FACTOR + PALETTE_BORDER * 2);
    }
}
