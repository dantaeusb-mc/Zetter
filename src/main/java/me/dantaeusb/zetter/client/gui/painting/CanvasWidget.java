package me.dantaeusb.zetter.client.gui.painting;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import me.dantaeusb.zetter.client.gui.PaintingScreen;
import me.dantaeusb.zetter.core.Helper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Widget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;

import static org.lwjgl.glfw.GLFW.*;

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

    /**
     * Drag-drawing
     *
     * @param mouseX
     * @param mouseY
     * @param button
     * @param dragX
     * @param dragY
     * @return
     */
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.isMouseOver(mouseX, mouseY)) {
            if (this.canvasDragging) {
                this.handleCanvasInteraction(mouseX, mouseY);
                return true;
            }

            return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }

        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    /**
     * Stop drag-drawing
     *
     * @param mouseX
     * @param mouseY
     * @param button
     * @return
     */
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.canvasDragging = false;

        return super.mouseReleased(mouseX, mouseY, button);
    }

    /**
     * Apply used tool
     *
     * @param mouseX
     * @param mouseY
     * @return
     */
    protected boolean handleCanvasInteraction(double mouseX, double mouseY) {
        final float canvasX = (float) ((mouseX - this.x) / (float) CANVAS_SCALE_FACTOR);
        final float canvasY = (float) ((mouseY - this.y) / (float) CANVAS_SCALE_FACTOR);

        this.parentScreen.getMenu().useTool(canvasX, canvasY);

        return true;
    }

    public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, PaintingScreen.PAINTING_RESOURCE);

        if (!this.parentScreen.isCanvasAvailable()) {
            return;
        }

        // A bit dumb but avoiding direct calls
        for (int i = 0; i < Helper.getResolution().getNumeric() * Helper.getResolution().getNumeric(); i++) {
            int localX = i % 16;
            int localY = i / 16;

            /**
             * @todo: better use canvas renderer because there's a texture ready to render
             */
            int color = this.parentScreen.getColorAt(i);
            int globalX = this.x + localX * CANVAS_SCALE_FACTOR;
            int globalY = this.y + localY * CANVAS_SCALE_FACTOR;

            fill(matrixStack, globalX, globalY, globalX + CANVAS_SCALE_FACTOR, globalY + CANVAS_SCALE_FACTOR, color);
        }

        if (mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width && mouseY < this.y + this.height) {
            GLFW.glfwSetInputMode(Minecraft.getInstance().getWindow().getWindow(), GLFW_CURSOR, GLFW_CURSOR_HIDDEN);
            int canvasX = (mouseX - this.x) / CANVAS_SCALE_FACTOR;
            int canvasY = (mouseY - this.y) / CANVAS_SCALE_FACTOR;

            this.renderCursor(matrixStack, canvasX, canvasY);
        } else {
            GLFW.glfwSetInputMode(Minecraft.getInstance().getWindow().getWindow(), GLFW_CURSOR, GLFW_CURSOR_NORMAL);
        }
    }

    public void renderCursor(PoseStack matrixStack, int localX, int localY) {
        int globalX1 = this.x + localX * CANVAS_SCALE_FACTOR - 1;
        int globalY1 = this.y + localY * CANVAS_SCALE_FACTOR - 1;
        int globalX2 = globalX1 + CANVAS_SCALE_FACTOR + 1;
        int globalY2 = globalY1 + CANVAS_SCALE_FACTOR + 1;

        this.hLine(matrixStack, globalX1, globalX2, globalY1, 0x80808080);
        this.hLine(matrixStack, globalX1, globalX2, globalY2, 0x80808080);
        this.vLine(matrixStack, globalX1, globalY1, globalY2, 0x80808080);
        this.vLine(matrixStack, globalX2, globalY1, globalY2, 0x80808080);
    }
}
