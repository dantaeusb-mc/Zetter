package com.dantaeusb.zetter.client.gui.painting;

import com.dantaeusb.zetter.client.gui.PaintingScreen;
import com.dantaeusb.zetter.core.Helper;
import com.dantaeusb.zetter.item.CanvasItem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.Widget;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;

import javax.annotation.Nullable;

public class CanvasWidget extends AbstractPaintingWidget implements Widget {
    private static final int CANVAS_SCALE_FACTOR = 5;
    private static final int size = Helper.getResolution().getNumeric() * CANVAS_SCALE_FACTOR;

    private boolean canvasDragging = false;

    public CanvasWidget(PaintingScreen parentScreen, int x, int y) {
        super(parentScreen, x, y, size, size, new TranslatableComponent("container.zetter.painting.canvas"));
    }

    @Override
    public @Nullable
    Component getTooltip(int mouseX, int mouseY) {
        return null;
    }

    /**
     * @todo: You can draw on the left-hand side by clicking outside canvas on the right (rounding error)
     * @param mouseX
     * @param mouseY
     */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int iMouseX = (int) mouseX;
        int iMouseY = (int) mouseY;

        if (this.isMouseOver(mouseX, mouseY)) {
            this.canvasDragging = true;
            this.handleCanvasInteraction(iMouseX, iMouseY);
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.canvasDragging && this.isMouseOver(mouseX, mouseY)) {
            int iMouseX = (int) mouseX;
            int iMouseY = (int) mouseY;

            this.handleCanvasInteraction(iMouseX, iMouseY);
            return true;
        }

        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.canvasDragging = false;

        return super.mouseReleased(mouseX, mouseY, button);
    }

    protected boolean handleCanvasInteraction(int iMouseX, int iMouseY) {
        int canvasX = (iMouseX - this.x) / CANVAS_SCALE_FACTOR;
        int canvasY = (iMouseY - this.y) / CANVAS_SCALE_FACTOR;

        this.parentScreen.useTool(canvasX, canvasY);

        return true;
    }

    public void render(PoseStack matrixStack) {
        if (!this.parentScreen.isCanvasAvailable()) {
            return;
        }

        for (int i = 0; i < Helper.getResolution().getNumeric() * Helper.getResolution().getNumeric(); i++) {
            int localX = i % 16;
            int localY = i / 16;

            /**
             * @todo: better use canvas renderer because there's a texture ready to render
             */
            int color = this.parentScreen.getColorAt(i);
            int globalX = this.x + localX * CANVAS_SCALE_FACTOR;
            int globalY = this.y + localY * CANVAS_SCALE_FACTOR;

            this.fillGradient(matrixStack, globalX, globalY, globalX + CANVAS_SCALE_FACTOR, globalY + CANVAS_SCALE_FACTOR, color, color);
        }
    }
}
