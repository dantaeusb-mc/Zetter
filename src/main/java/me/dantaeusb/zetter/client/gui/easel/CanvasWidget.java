package me.dantaeusb.zetter.client.gui.easel;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.client.gui.EaselScreen;
import me.dantaeusb.zetter.client.renderer.CanvasRenderer;
import me.dantaeusb.zetter.painting.Tools;
import me.dantaeusb.zetter.painting.parameters.AbstractToolParameters;
import me.dantaeusb.zetter.painting.parameters.SizeParameterHolder;
import me.dantaeusb.zetter.painting.tools.AbstractTool;
import me.dantaeusb.zetter.storage.CanvasData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;

import static com.mojang.blaze3d.platform.GlConst.GL_FUNC_ADD;
import static com.mojang.blaze3d.platform.GlConst.GL_FUNC_SUBTRACT;
import static org.lwjgl.glfw.GLFW.*;

public class CanvasWidget extends AbstractPaintingWidget implements Renderable {
    public static final int SIZE = 128;

    public static final ResourceLocation PAINTING_CHECKER_RESOURCE = new ResourceLocation(Zetter.MOD_ID, "textures/gui/easel/checker.png");

    private boolean canvasDragging = false;

    private double scrollDistance = 0d;
    private long scrollTimestamp = 0;

    public CanvasWidget(EaselScreen parentScreen, int x, int y) {
        super(parentScreen, x, y, SIZE, SIZE, Component.translatable("container.zetter.painting.canvas"));
    }

    private int getCanvasScale() {
        return this.parentScreen.getMenu().getCanvasScaleFactor() * 2;
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
     * Zoom zoom
     *
     * @param mouseX
     * @param mouseY
     * @param delta
     * @return
     */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (this.isMouseOver(mouseX, mouseY)) {
            this.scrollDistance += delta;
            this.scrollTimestamp = System.currentTimeMillis();

            if (this.scrollDistance <= -1.5d) {
                this.parentScreen.getMenu().decreaseCanvasScale();
                this.scrollDistance = 0d;
            } else if (this.scrollDistance >= 1.5d) {
                this.parentScreen.getMenu().increaseCanvasScale();
                this.scrollDistance = 0d;
            }

            return true;
        }

        return false;
    }

    public void tick() {
        if (this.scrollDistance != 0d) {
            if (System.currentTimeMillis() - this.scrollTimestamp > 1500) {
                this.scrollDistance = 0;
            }
        }
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
        final float canvasX = (float) ((mouseX - this.getX() - this.parentScreen.getMenu().getCanvasOffsetX()) / (float) this.getCanvasScale());
        final float canvasY = (float) ((mouseY - this.getY() - this.parentScreen.getMenu().getCanvasOffsetY()) / (float) this.getCanvasScale());

        this.parentScreen.getMenu().useTool(canvasX, canvasY);

        return true;
    }

    public void renderWidget(@NotNull PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        if (!this.parentScreen.getMenu().isCanvasAvailable()) {
            GLFW.glfwSetInputMode(Minecraft.getInstance().getWindow().getWindow(), GLFW_CURSOR, GLFW_CURSOR_NORMAL);
            return;
        }

        final int scale = (int) this.parentScreen.getMinecraft().getWindow().getGuiScale();
        // Ah yes, OpenGL starts from bottom
        final int windowHeight = this.parentScreen.getMinecraft().getWindow().getHeight();

        RenderSystem.enableScissor(
            this.getX() * scale,
            windowHeight - (this.getY() + this.height) * scale,
            this.width * scale,
            this.height * scale
        );

        this.renderCheckerboard(matrixStack);

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, EaselScreen.PAINTING_RESOURCE);

        String canvasCode = this.parentScreen.getMenu().getCanvasCode();
        CanvasData canvasData = this.parentScreen.getMenu().getCanvasData();

        matrixStack.pushPose();
        matrixStack.translate(this.getX() + this.parentScreen.getMenu().getCanvasOffsetX(), this.getY() + this.parentScreen.getMenu().getCanvasOffsetY(), 1.0F);
        matrixStack.scale(this.getCanvasScale(), this.getCanvasScale(), 1.0F);

        MultiBufferSource.BufferSource renderTypeBufferImpl = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
        CanvasRenderer.getInstance().renderCanvas(matrixStack, renderTypeBufferImpl, canvasCode, canvasData, 0xF000F0);
        renderTypeBufferImpl.endBatch();

        matrixStack.popPose();

        if (mouseX >= this.getX()
            && mouseY >= this.getY()
            && mouseX < this.getX() + this.width
            && mouseY < this.getY() + this.height
        ) {
            if (this.parentScreen.getMenu().getCurrentTool().getTool() == Tools.HAND.getTool()) {
                GLFW.glfwSetInputMode(Minecraft.getInstance().getWindow().getWindow(), GLFW_CURSOR, GLFW_CURSOR_NORMAL);
            } else {
                double canvasX = (mouseX - this.getX() - this.parentScreen.getMenu().getCanvasOffsetX()) / (double) this.getCanvasScale();
                double canvasY = (mouseY - this.getY() - this.parentScreen.getMenu().getCanvasOffsetY()) / (double) this.getCanvasScale();

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
     *
     * @param matrixStack
     */
    private void renderCheckerboard(PoseStack matrixStack) {
        final int offsetX = Math.abs(this.getCanvasScale() * 2 - this.parentScreen.getMenu().getCanvasOffsetX() % (this.getCanvasScale() * 2));
        final int offsetY = Math.abs(this.getCanvasScale() * 2 - this.parentScreen.getMenu().getCanvasOffsetY() % (this.getCanvasScale() * 2));

        final int width = this.width + this.getCanvasScale() * 4;
        final int height = this.height + this.getCanvasScale() * 4;

        float x1 = this.getX() - offsetX;
        float x2 = x1 + width;
        float y1 = this.getY() - offsetY;
        float y2 = y1 + height;

        // Size of one copy
        float uvScale = this.getCanvasScale() * 2f;

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
        bufferBuilder.vertex(matrix, x1, y2, 0).uv(u1, v2).endVertex();
        bufferBuilder.vertex(matrix, x2, y2, 0).uv(u2, v2).endVertex();
        bufferBuilder.vertex(matrix, x2, y1, 0).uv(u2, v1).endVertex();
        bufferBuilder.vertex(matrix, x1, y1, 0).uv(u1, v1).endVertex();
        BufferUploader.drawWithShader(bufferBuilder.end());

        matrixStack.popPose();
    }

    private void renderCursor(PoseStack matrixStack, double canvasX, double canvasY) {
        AbstractToolParameters toolParameters = this.parentScreen.getMenu().getCurrentToolParameters();
        AbstractTool.ToolShape shape = this.parentScreen.getMenu().getCurrentTool().getTool().getShape(toolParameters);

        RenderSystem.blendEquation(GL_FUNC_SUBTRACT);

        if (shape == null) {
            GLFW.glfwSetInputMode(Minecraft.getInstance().getWindow().getWindow(), GLFW_CURSOR, GLFW_CURSOR_HIDDEN);

            int radius = this.getCanvasScale() * 2;

            if (toolParameters instanceof SizeParameterHolder) {
                radius = Math.round(this.getCanvasScale() * ((SizeParameterHolder) toolParameters).getSize());
            }

            if (radius < 4) {
                radius = 4;
            }

            int globalX = this.getX() + this.parentScreen.getMenu().getCanvasOffsetX() + (int) Math.floor(canvasX * this.getCanvasScale()) - 1;
            int globalY = this.getY() + this.parentScreen.getMenu().getCanvasOffsetY() + (int) Math.floor(canvasY * this.getCanvasScale()) - 1;

            hLine(matrixStack, globalX - radius, globalX - 2, globalY, 0x80808080);
            hLine(matrixStack, globalX + radius, globalX + 2, globalY, 0x80808080);

            vLine(matrixStack, globalX, globalY - radius, globalY - 2, 0x80808080);
            vLine(matrixStack, globalX, globalY + radius, globalY + 2, 0x80808080);
        } else {
            GLFW.glfwSetInputMode(Minecraft.getInstance().getWindow().getWindow(), GLFW_CURSOR, GLFW_CURSOR_NORMAL);

            // Offset for the current shape, in canvas pixels
            final int offset = -(shape.getSize() / 2);

            int globalX = this.getX() + this.parentScreen.getMenu().getCanvasOffsetX() + (int) Math.floor(canvasX) * this.getCanvasScale();
            int globalY = this.getY() + this.parentScreen.getMenu().getCanvasOffsetY() + (int) Math.floor(canvasY) * this.getCanvasScale();

            for (AbstractTool.ShapeLine line : shape.getLines()) {
                // Relative positions from the cursor "center" in canvas pixels
                // Center is
                int posX = (line.posX() + offset) * this.getCanvasScale();
                int posY = (line.posY() + offset) * this.getCanvasScale();
                int length = line.length() * this.getCanvasScale();

                if (line.direction() == AbstractTool.ShapeLine.LineDirection.HORIZONTAL) {
                    // Wrap around top-left
                    if (posX <= 0) {
                        posX--;

                        if (posX + length > 0) {
                            length++;
                        }
                    }

                    if (posY <= 0) {
                        posY--;
                    }

                    final int minX = globalX + posX;
                    this.hLine(matrixStack, minX, minX + length, globalY + posY, 0x80808080);
                } else {
                    // Wrap around bottom-right
                    if (posX <= 0) {
                        posX--;
                    }

                    if (posY <= 0) {
                        posY--;

                        if (posY + length > 0) {
                            length++;
                        }
                    }

                    final int minY = globalY + posY;
                    vLine(matrixStack, globalX + posX, minY, minY + length, 0x80808080);
                }
            }
        }

        RenderSystem.blendEquation(GL_FUNC_ADD);

        /*this.hLine(matrixStack, globalX1, globalX2, globalY1, 0x80808080);
        this.hLine(matrixStack, globalX1, globalX2, globalY2, 0x80808080);
        this.vLine(matrixStack, globalX1, globalY1, globalY2, 0x80808080);
        this.vLine(matrixStack, globalX2, globalY1, globalY2, 0x80808080);*/
    }
}
