package me.dantaeusb.zetter.client.gui.painting;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Matrix4f;
import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.client.gui.PaintingScreen;
import me.dantaeusb.zetter.client.renderer.CanvasRenderer;
import me.dantaeusb.zetter.painting.Tools;
import me.dantaeusb.zetter.painting.parameters.AbstractToolParameters;
import me.dantaeusb.zetter.painting.parameters.SizeInterface;
import me.dantaeusb.zetter.painting.tools.AbstractTool;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Widget;
import net.minecraft.client.renderer.GameRenderer;
import me.dantaeusb.zetter.storage.CanvasData;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;

import static org.lwjgl.glfw.GLFW.*;

public class CanvasWidget extends AbstractPaintingWidget implements Widget {
    private static final int SIZE = 128;

    public static final ResourceLocation PAINTING_CHECKER_RESOURCE = new ResourceLocation("zetter", "textures/gui/painting/checker.png");

    private boolean canvasDragging = false;

    // @todo: move to menu as widget might be recreated
    private int canvasScale = 6;

    private int canvasOffsetX = 0;
    private int canvasOffsetY = 0;

    public CanvasWidget(PaintingScreen parentScreen, int x, int y) {
        super(parentScreen, x, y, SIZE, SIZE, new TranslatableComponent("container.zetter.painting.canvas"));

        if (parentScreen.getMenu().getCanvasData() != null) {
            this.canvasOffsetX = (SIZE - (parentScreen.getMenu().getCanvasData().getWidth() * this.canvasScale)) / 2;
            this.canvasOffsetY = (SIZE - (parentScreen.getMenu().getCanvasData().getHeight() * this.canvasScale)) / 2;
        }
    }

    /**
     * Update Canvas offset - usually with Hand tool
     * @param offsetX
     * @param offsetY
     */
    public void updateCanvasOffset(int offsetX, int offsetY) {
        if (parentScreen.getMenu().getCanvasData() == null) {
            return;
        }

        final int width = parentScreen.getMenu().getCanvasData().getWidth() * this.canvasScale;
        final int minOffsetX = -width + SIZE / 2;
        final int maxOffsetX = SIZE / 2;

        final int height = parentScreen.getMenu().getCanvasData().getHeight() * this.canvasScale;
        final int minOffsetY = -height + SIZE / 2;
        final int maxOffsetY = SIZE / 2;

        this.canvasOffsetX = Math.max(minOffsetX, Math.min(offsetX, maxOffsetX));
        this.canvasOffsetY = Math.max(minOffsetY, Math.min(offsetY, maxOffsetY));
    }

    public int getCanvasOffsetX() {
        return this.canvasOffsetX;
    }

    public int getCanvasOffsetY() {
        return this.canvasOffsetY;
    }

    public int getCanvasScale() {
        return this.canvasScale;
    }

    @Override
    public @Nullable
    Component getTooltip(int mouseX, int mouseY) {
        return null;
    }

    /**
     * @param mouseX
     * @param mouseY
     */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.isMouseOver(mouseX, mouseY)) {
            this.canvasDragging = true;
            this.handleCanvasInteraction(mouseX, mouseY);
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
     * Apply used tool. Could apply outside the canvas!
     * It is for tool to decide if it's going to apply interaction
     *
     * @param mouseX
     * @param mouseY
     * @return
     */
    protected boolean handleCanvasInteraction(double mouseX, double mouseY) {
        final float canvasX = (float) ((mouseX - this.x - this.canvasOffsetX) / (float) this.canvasScale);
        final float canvasY = (float) ((mouseY - this.y - this.canvasOffsetY) / (float) this.canvasScale);

        this.parentScreen.getMenu().useTool(canvasX, canvasY);

        return true;
    }

    public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        if (!this.parentScreen.getMenu().isCanvasAvailable()) {
            GLFW.glfwSetInputMode(Minecraft.getInstance().getWindow().getWindow(), GLFW_CURSOR, GLFW_CURSOR_NORMAL);
            return;
        }

        final int scale = (int) this.parentScreen.getMinecraft().getWindow().getGuiScale();
        // Ah yes, OpenGL starts from bottom
        final int windowHeight = this.parentScreen.getMinecraft().getWindow().getHeight();

        RenderSystem.enableScissor(
                this.x * scale,
                windowHeight - (this.y + this.height) * scale,
                this.width * scale,
                this.height * scale
        );

        this.renderCheckerboard(matrixStack);

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, PaintingScreen.PAINTING_RESOURCE);

        String canvasCode = this.parentScreen.getMenu().getCanvasCode();
        CanvasData canvasData = this.parentScreen.getMenu().getCanvasData();

        matrixStack.pushPose();
        matrixStack.translate(this.x + this.canvasOffsetX, this.y + this.canvasOffsetY, 1.0F);
        matrixStack.scale(this.canvasScale, this.canvasScale, 1.0F);

        MultiBufferSource.BufferSource renderTypeBufferImpl = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
        CanvasRenderer.getInstance().renderCanvas(matrixStack, renderTypeBufferImpl, canvasCode, canvasData, 0xF000F0);
        renderTypeBufferImpl.endBatch();

        matrixStack.popPose();

        if (    mouseX >= this.x
                && mouseY >= this.y
                && mouseX < this.x + this.width
                && mouseY < this.y + this.height
        ) {
            if (this.parentScreen.getMenu().getCurrentTool().getTool() == Tools.HAND.getTool()) {
                GLFW.glfwSetInputMode(Minecraft.getInstance().getWindow().getWindow(), GLFW_CURSOR, GLFW_CURSOR_NORMAL);
            } else {
                GLFW.glfwSetInputMode(Minecraft.getInstance().getWindow().getWindow(), GLFW_CURSOR, GLFW_CURSOR_HIDDEN);
                double canvasX = (mouseX - this.x - this.canvasOffsetX) / (double) this.canvasScale;
                double canvasY = (mouseY - this.y - this.canvasOffsetY) / (double) this.canvasScale;

                this.renderCursor(matrixStack, canvasX, canvasY);
            }
        } else {
            GLFW.glfwSetInputMode(Minecraft.getInstance().getWindow().getWindow(), GLFW_CURSOR, GLFW_CURSOR_NORMAL);
        }

        RenderSystem.disableScissor();
    }

    /**
     * Renders checkerboard background according to scale and
     * canvas position
     * @param matrixStack
     */
    private void renderCheckerboard(PoseStack matrixStack) {
        final int offsetX = Math.abs(this.canvasScale * 2 - this.canvasOffsetX % (this.canvasScale * 2));
        final int offsetY = Math.abs(this.canvasScale * 2 - this.canvasOffsetY % (this.canvasScale * 2));

        final int width = this.width + this.getCanvasScale() * 4;
        final int height = this.height + this.getCanvasScale() * 4;

        float x1 = this.x - offsetX;
        float x2 = x1 + width;
        float y1 = this.y - offsetY;
        float y2 = y1 + height;

        // Size of one copy
        float uvScale = this.canvasScale * 2f;

        float u1 = 0f;
        float u2 = width / uvScale;
        float v1 = 0f;
        float v2 = height / uvScale;

        matrixStack.pushPose();
        Matrix4f matrix = matrixStack.last().pose();

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, PAINTING_CHECKER_RESOURCE);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bufferBuilder.vertex(matrix, x1, y2, this.getBlitOffset()).uv(u1, v2).endVertex();
        bufferBuilder.vertex(matrix, x2, y2, this.getBlitOffset()).uv(u2, v2).endVertex();
        bufferBuilder.vertex(matrix, x2, y1, this.getBlitOffset()).uv(u2, v1).endVertex();
        bufferBuilder.vertex(matrix, x1, y1, this.getBlitOffset()).uv(u1, v1).endVertex();
        bufferBuilder.end();
        BufferUploader.end(bufferBuilder);

        matrixStack.popPose();
    }

    private void renderCursor(PoseStack matrixStack, double canvasX, double canvasY) {

        AbstractToolParameters toolParameters = this.parentScreen.getMenu().getCurrentToolParameters();
        AbstractTool.ToolShape shape = this.parentScreen.getMenu().getCurrentTool().getTool().getShape(toolParameters);

        if (shape == null) {
            int radius = this.canvasScale * 2;

            if (toolParameters instanceof SizeInterface) {
                radius = Math.round(this.canvasScale * ((SizeInterface) toolParameters).getSize());
            }

            if (radius < 4) {
                radius = 4;
            }

            int globalX = this.x + this.canvasOffsetX + (int) Math.floor(canvasX * this.canvasScale) - 1;
            int globalY = this.y + this.canvasOffsetY + (int) Math.floor(canvasY * this.canvasScale) - 1;

            this.hLine(matrixStack, globalX - radius, globalX - 2, globalY, 0x80808080);
            this.hLine(matrixStack, globalX + radius, globalX + 2, globalY, 0x80808080);

            this.vLine(matrixStack, globalX, globalY - radius, globalY - 2, 0x80808080);
            this.vLine(matrixStack, globalX, globalY + radius, globalY + 2, 0x80808080);
        } else {
            int globalX = this.x + this.canvasOffsetX + (int) Math.floor(canvasX) * this.canvasScale - 1;
            int globalY = this.y + this.canvasOffsetY + (int) Math.floor(canvasY) * this.canvasScale - 1;

            for (AbstractTool.ShapeLine line : shape.getLines()) {
                int posX = line.posX() * this.canvasScale;
                int posY = line.posY() * this.canvasScale;
                int length = line.length() * this.canvasScale;

                // Add + 1 to wrap pixel around
                if (posX > 0) {
                    posX++;
                }

                if (posY > 0) {
                    posY++;
                }

                if (line.direction() == AbstractTool.ShapeLine.LineDirection.HORIZONTAL) {
                    if (posX + length > 0) {
                        length++;
                    }

                    this.hLine(matrixStack, globalX + posX, globalX + posX + length, globalY + posY, 0x80808080);
                } else {
                    if (posY + length > 0) {
                        length++;
                    }

                    this.vLine(matrixStack, globalX + posX, globalY + posY, globalY + posY + length, 0x80808080);
                }
            }
        }

        /*this.hLine(matrixStack, globalX1, globalX2, globalY1, 0x80808080);
        this.hLine(matrixStack, globalX1, globalX2, globalY2, 0x80808080);
        this.vLine(matrixStack, globalX1, globalY1, globalY2, 0x80808080);
        this.vLine(matrixStack, globalX2, globalY1, globalY2, 0x80808080);*/
    }
}
