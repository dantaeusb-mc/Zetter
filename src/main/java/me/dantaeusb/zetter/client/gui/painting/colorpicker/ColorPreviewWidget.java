package me.dantaeusb.zetter.client.gui.painting.colorpicker;

import me.dantaeusb.zetter.client.gui.PaintingScreen;
import me.dantaeusb.zetter.client.gui.painting.AbstractPaintingWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;

public class ColorPreviewWidget extends AbstractPaintingWidget implements Renderable {
    final static int COLOR_PREVIEW_TOOL_HEIGHT = 26;
    final static int COLOR_PREVIEW_TOOL_WIDTH = 26;

    final static int COLOR_PREVIEW_TOOL_BLOCK_HEIGHT = 16;
    final static int COLOR_PREVIEW_TOOL_BLOCK_WIDTH = 16;

    public static final int SWAP_HOTKEY = GLFW.GLFW_KEY_X;

    public ColorPreviewWidget(PaintingScreen parentScreen, int x, int y) {
        super(
            parentScreen, x, y,
            COLOR_PREVIEW_TOOL_WIDTH,
            COLOR_PREVIEW_TOOL_HEIGHT,
            Component.translatable("container.zetter.painting.color_preview")
        );
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

        if (!this.isMouseOver(mouseX, mouseY)) {
            return false;
        }

        // Top right corner where the swap icon is
        final boolean swap = iMouseX > this.getX() + COLOR_PREVIEW_TOOL_HEIGHT + 1 &&
               iMouseY < this.getY() + COLOR_PREVIEW_TOOL_BLOCK_WIDTH + 1;

        if (!swap) {
            return false;
        }

        // @todo: Do swap

        return true;
    }

    protected void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        final int PALETTE_CURRENT_COLOR_SWAP_TOOL_U = 120;
        final int PALETTE_CURRENT_COLOR_SWAP_TOOL_V = 89;

        final int offset = COLOR_PREVIEW_TOOL_WIDTH - COLOR_PREVIEW_TOOL_BLOCK_WIDTH;

        guiGraphics.blit(
            PAINTING_WIDGETS_TEXTURE_RESOURCE,
            this.getX() + offset,
            this.getY() + offset,
            PALETTE_CURRENT_COLOR_SWAP_TOOL_U,
            PALETTE_CURRENT_COLOR_SWAP_TOOL_V,
            COLOR_PREVIEW_TOOL_BLOCK_WIDTH,
            COLOR_PREVIEW_TOOL_BLOCK_HEIGHT
        );

        int bgColor = this.parentScreen.getCurrentColor().getARGB();

        guiGraphics.fill(
            this.getX() + offset + 1,
            this.getY() + offset + 1,
            this.getX() + COLOR_PREVIEW_TOOL_BLOCK_WIDTH - 1,
            this.getY() + COLOR_PREVIEW_TOOL_BLOCK_HEIGHT - 1,
            bgColor
        );

        guiGraphics.blit(
            PAINTING_WIDGETS_TEXTURE_RESOURCE,
            this.getX(),
            this.getY(),
            PALETTE_CURRENT_COLOR_SWAP_TOOL_U,
            PALETTE_CURRENT_COLOR_SWAP_TOOL_V,
            COLOR_PREVIEW_TOOL_BLOCK_WIDTH,
            COLOR_PREVIEW_TOOL_BLOCK_HEIGHT
        );

        int color = this.parentScreen.getCurrentColor().getARGB();

        guiGraphics.fill(
            this.getX() + 1,
            this.getY() + 1,
            this.getX() + COLOR_PREVIEW_TOOL_BLOCK_WIDTH - 1,
            this.getY() + COLOR_PREVIEW_TOOL_BLOCK_HEIGHT - 1,
            color
        );
    }
}
