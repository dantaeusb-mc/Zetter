package me.dantaeusb.zetter.client.gui.painting.canvaslayer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.capability.canvastracker.CanvasTracker;
import me.dantaeusb.zetter.client.gui.PaintingScreen;
import me.dantaeusb.zetter.client.gui.painting.util.state.CanvasOverlayState;
import me.dantaeusb.zetter.client.renderer.CanvasRenderer;
import me.dantaeusb.zetter.core.Helper;
import me.dantaeusb.zetter.painting.Tool;
import me.dantaeusb.zetter.painting.parameters.AbstractToolParameters;
import me.dantaeusb.zetter.painting.parameters.SizeParameterHolder;
import me.dantaeusb.zetter.painting.tools.AbstractTool;
import me.dantaeusb.zetter.storage.AbstractCanvasData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector2d;
import org.joml.Vector2i;
import org.lwjgl.glfw.GLFW;

import static org.lwjgl.glfw.GLFW.*;

public class CanvasLayerOverlay extends CanvasLayerAbstract {
    public static final ResourceLocation PAINTING_CHECKER_RESOURCE = new ResourceLocation(Zetter.MOD_ID, "textures/gui/painting/checker.png");

    private @Nullable Vector2d dragStart;
    private @Nullable Vector2d dragCanvasOffset;

    private double scrollDistance = 0d;
    private long scrollTimestamp = 0;

    public CanvasLayerOverlay(PaintingScreen parentScreen) {
        super(parentScreen);
    }

    /**
     * Apply used tool. Could apply outside the canvas!
     * It is for tool to decide if it's going to apply interaction
     *
     * @param mouseX
     * @param mouseY
     * @return
     */
    @Override
    protected boolean handleCanvasInteraction(double mouseX, double mouseY, int button) {
        Vector2i canvasZeroCoordinates = this.getCanvasZeroCoordinates();

        if (canvasZeroCoordinates == null) {
            return false;
        }

        // To be consistent with cursor rendering, we need to round it to integer
        final int iMouseX = (int) mouseX;
        final int iMouseY = (int) mouseY;

        if (this.parentScreen.getPaletteState().currentTool().equals(Tool.HAND)) {
            Vector2d diff = this.dragStart != null
                ? new Vector2d(iMouseX - this.dragStart.x, iMouseY - this.dragStart.y)
                : new Vector2d(0, 0);

            if (this.dragCanvasOffset == null) {
                this.dragCanvasOffset = new Vector2d(
                    this.parentScreen.getEaselState().canvasOverlayState().canvasOffsetX(),
                    this.parentScreen.getEaselState().canvasOverlayState().canvasOffsetY()
                );
            }

            // Hand tool is special, it just moves canvas
            this.parentScreen.setEaselState(
                this.parentScreen.getEaselState().withCanvasOverlayState(
                    this.parentScreen.getEaselState().canvasOverlayState().withCanvasOffset(
                        (int) (this.dragCanvasOffset.x + (diff.x / this.getCanvasScale())),
                        (int) (this.dragCanvasOffset.y + (diff.y / this.getCanvasScale()))
                    )
                )
            );
            return true;
        }

        this.parentScreen.useTool(
            (float) (iMouseX - canvasZeroCoordinates.x) / this.getCanvasScale(),
            (float) (iMouseY - canvasZeroCoordinates.y) / this.getCanvasScale()
        );

        return true;
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

            if (this.scrollDistance <= -1.0d && this.canDecreaseCanvasScale()) {
                this.decreaseCanvasScale();
                this.scrollDistance = 0d;
            } else if (this.scrollDistance >= 1.0d && this.canIncreaseCanvasScale()) {
                this.increaseCanvasScale();
                this.scrollDistance = 0d;
            }

            return true;
        }

        return false;
    }

    /**
     * @param mouseX
     * @param mouseY
     */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.isMouseOver(mouseX, mouseY)) {
            this.dragStart = new Vector2d(mouseX, mouseY);
            this.handleCanvasInteraction(mouseX, mouseY, button);
            return true;
        }

        return false;
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
            if (this.dragStart != null) {
                this.handleCanvasInteraction(mouseX, mouseY, button);
                return true;
            }

            return false;
        }

        return false;
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
        this.dragStart = null;
        this.dragCanvasOffset = null;
        return false;
    }

    private boolean canDecreaseCanvasScale() {
        return this.parentScreen.getEaselState().canvasOverlayState().canvasScale() > CanvasOverlayState.MIN_SCALE;
    }

    private void decreaseCanvasScale() {
        this.parentScreen.setEaselState(this.parentScreen.getEaselState().decreaseCanvasScale());
    }

    private boolean canIncreaseCanvasScale() {
        return this.parentScreen.getEaselState().canvasOverlayState().canvasScale() < CanvasOverlayState.MAX_SCALE;
    }

    private void increaseCanvasScale() {
        this.parentScreen.setEaselState(this.parentScreen.getEaselState().increaseCanvasScale());
    }

    public void tick() {
        if (this.scrollDistance != 0d) {
            if (System.currentTimeMillis() - this.scrollTimestamp > 750) {
                this.scrollDistance = 0;
            }
        }
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= this.leftPos
            && mouseY >= this.topPos
            && mouseX < this.leftPos + this.width
            && mouseY < this.topPos + this.height;
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        Minecraft minecraft = this.parentScreen.getMinecraft();

        final int canvasScale = this.getCanvasScale();
        final int guiScale = (int) minecraft.getWindow().getGuiScale();
        final int scaleRoundingOffsetX = minecraft.getWindow().getWidth() % guiScale;
        final int scaleRoundingOffsetY = minecraft.getWindow().getHeight() % guiScale;

        if (minecraft.level == null) {
            return;
        }

        String canvasCode = this.parentScreen.getCanvasHolderEntity().getCanvasCode();
        if (canvasCode == null) {
            return;
        }

        CanvasTracker canvasTracker = Helper.getLevelCanvasTracker(minecraft.level);
        AbstractCanvasData canvasData = canvasTracker.getCanvasData(canvasCode);

        if (canvasData == null) {
            return;
        }

        Vector2i canvasZeroCoordinates = this.getCanvasZeroCoordinates();
        assert canvasZeroCoordinates != null;

        final int canvas0X = canvasZeroCoordinates.x;
        final int canvas0Y = canvasZeroCoordinates.y;

        this.renderOverlayBorders(guiGraphics);

        RenderSystem.enableScissor(
            (this.leftPos + 4) * guiScale - scaleRoundingOffsetX,
            (this.topPos + 4) * guiScale - scaleRoundingOffsetY,
            (this.width - 8) * guiScale,
            (this.height - 8) * guiScale
        );

        this.renderOverlayCheckerboard(guiGraphics, canvas0X, canvas0Y);

        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        poseStack.translate(canvas0X, canvas0Y, 0.0f);
        poseStack.scale(canvasScale, canvasScale, 1.0f);

        MultiBufferSource.BufferSource renderTypeBufferImpl = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
        CanvasRenderer.getInstance().renderCanvas(poseStack, renderTypeBufferImpl, canvasCode, canvasData, 0xF000F0);
        renderTypeBufferImpl.endBatch();

        poseStack.popPose();

        if (mouseX >= this.leftPos
            && mouseY >= this.topPos
            && mouseX < this.leftPos + this.width
            && mouseY < this.topPos + this.height
        ) {
            if (this.parentScreen.getPaletteState().currentTool() == Tool.HAND) {
                GLFW.glfwSetInputMode(Minecraft.getInstance().getWindow().getWindow(), GLFW_CURSOR, GLFW_CURSOR_NORMAL);
            } else {
                GLFW.glfwSetInputMode(Minecraft.getInstance().getWindow().getWindow(), GLFW_CURSOR, GLFW_CURSOR_NORMAL);

                double canvasX = (double) (mouseX - canvas0X) / canvasScale;
                double canvasY = (double) (mouseY - canvas0Y) / canvasScale;

                this.renderCursor(guiGraphics, canvasX, canvasY);
            }
        } else {
            GLFW.glfwSetInputMode(Minecraft.getInstance().getWindow().getWindow(), GLFW_CURSOR, GLFW_CURSOR_NORMAL);
        }

        RenderSystem.disableScissor();
    }

    /**
     * Renders box with checkerboard background according to scale and
     * canvas position
     *
     * @param guiGraphics
     */
    private void renderOverlayBorders(GuiGraphics guiGraphics) {
        guiGraphics.blitNineSliced(
            PaintingScreen.PAINTING_GUI_TEXTURE_RESOURCE,
            this.leftPos,
            this.topPos,
            this.width,
            this.height,
            4,
            4,
            48,
            48,
            195,
            0
        );
    }

    /**
     * @param guiGraphics
     * @param canvasX     – canvas X position in GUI scaled pixels, from top of the screen
     * @param canvasY     – canvas Y position in GUI scaled pixels, from left of the screen
     */
    private void renderOverlayCheckerboard(GuiGraphics guiGraphics, int canvasX, int canvasY) {
        final int canvasScale = this.getCanvasScale();
        final int checkerboardScale = canvasScale * 4;

        // Checkerboard is 4x4
        final int offsetX = Mth.abs(checkerboardScale - (canvasX - this.leftPos) % checkerboardScale);
        final int offsetY = Mth.abs(checkerboardScale - (canvasY - this.topPos) % checkerboardScale);

        // Add one full checkerboard to the right and bottom
        final int width = this.width + checkerboardScale;
        final int height = this.height + checkerboardScale;

        float x1 = this.leftPos - offsetX;
        float x2 = x1 + width;
        float y1 = this.topPos - offsetY;
        float y2 = y1 + height;

        // Size of one copy
        float uvScale = canvasScale * 2.0f;

        float u1 = 0f;
        float u2 = width / uvScale;
        float v1 = 0f;
        float v2 = height / uvScale;

        Matrix4f matrix = guiGraphics.pose().last().pose();

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, PAINTING_CHECKER_RESOURCE);

        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bufferBuilder.vertex(matrix, x1, y2, 0).uv(u1, v2).endVertex();
        bufferBuilder.vertex(matrix, x2, y2, 0).uv(u2, v2).endVertex();
        bufferBuilder.vertex(matrix, x2, y1, 0).uv(u2, v1).endVertex();
        bufferBuilder.vertex(matrix, x1, y1, 0).uv(u1, v1).endVertex();
        BufferUploader.drawWithShader(bufferBuilder.end());
    }

    private void renderCursor(GuiGraphics guiGraphics, double canvasX, double canvasY) {
        Tool tool = this.parentScreen.getPaletteState().currentTool();
        AbstractToolParameters toolParameters = this.parentScreen.getToolsParameters().getToolParameters(tool);
        AbstractTool.ToolShape shape = tool.getTool().getShape(toolParameters);

        Vector2i canvasZeroCoordinates = this.getCanvasZeroCoordinates();

        assert canvasZeroCoordinates != null;
        int globalX = canvasZeroCoordinates.x + Mth.floor(canvasX) * this.getCanvasScale();
        int globalY = canvasZeroCoordinates.y + Mth.floor(canvasY) * this.getCanvasScale();

        if (shape == null) {
            int radius = this.getCanvasScale() * 2;

            if (toolParameters instanceof SizeParameterHolder) {
                radius = Math.round(this.getCanvasScale() * ((SizeParameterHolder) toolParameters).getSize());
            }

            if (radius < 4) {
                radius = 4;
            }

            this.drawCursor(guiGraphics, radius, globalX - 1, globalY - 1, this.getCanvasScale());
        } else {
            this.drawCursor(guiGraphics, shape, globalX, globalY, this.getCanvasScale());
        }
    /*this.hLine(matrixStack, globalX1, globalX2, globalY1, 0x80808080);
    this.hLine(matrixStack, globalX1, globalX2, globalY2, 0x80808080);
    this.vLine(matrixStack, globalX1, globalY1, globalY2, 0x80808080);
    this.vLine(matrixStack, globalX2, globalY1, globalY2, 0x80808080);*/
    }

    private @Nullable Vector2i getCanvasZeroCoordinates() {
        Minecraft minecraft = this.parentScreen.getMinecraft();

        final CanvasOverlayState canvasOverlayState = this.parentScreen.getEaselState().canvasOverlayState();

        String canvasCode = this.parentScreen.getCanvasHolderEntity().getCanvasCode();

        final int canvasScale = this.getCanvasScale();

        CanvasTracker canvasTracker = Helper.getLevelCanvasTracker(minecraft.level);
        AbstractCanvasData canvasData = canvasTracker.getCanvasData(canvasCode);

        if (canvasData == null) {
            return null;
        }

        final int canvasWidth = canvasData.getWidth() * canvasScale;
        final int canvasHeight = canvasData.getHeight() * canvasScale;

        final int canvasX0 = Math.round(this.centerX - (canvasWidth / 2.0f) + canvasOverlayState.canvasOffsetX() * canvasScale);
        final int canvasY0 = Math.round(this.centerY - (canvasHeight / 2.0f) + canvasOverlayState.canvasOffsetY() * canvasScale);

        return new Vector2i(canvasX0, canvasY0);
    }

    private int getCanvasScale() {
        return this.parentScreen.getEaselState().canvasOverlayState().canvasScale();
    }
}
