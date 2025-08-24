package me.dantaeusb.zetter.client.gui.painting.canvaslayer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.math.Axis;
import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.capability.canvastracker.CanvasTracker;
import me.dantaeusb.zetter.client.gui.PaintingScreen;
import me.dantaeusb.zetter.client.renderer.CanvasRenderer;
import me.dantaeusb.zetter.core.Helper;
import me.dantaeusb.zetter.painting.Tool;
import me.dantaeusb.zetter.painting.parameters.AbstractToolParameters;
import me.dantaeusb.zetter.painting.parameters.SizeParameterHolder;
import me.dantaeusb.zetter.painting.tools.AbstractTool;
import me.dantaeusb.zetter.storage.AbstractCanvasData;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

import java.util.Optional;

import static org.lwjgl.glfw.GLFW.*;

public class CanvasLayerImmersive extends CanvasLayerAbstract {
    public CanvasLayerImmersive(PaintingScreen parentScreen) {
        super(parentScreen);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        return false;
    }

    @Override
    public boolean isMouseOver(double pMouseX, double pMouseY) {
        return true;
    }

    /**
     * @param mouseX
     * @param mouseY
     */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.isMouseOver(mouseX, mouseY)) {
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
        this.handleCanvasInteraction(mouseX, mouseY, button);
        return true;
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
        return false;
    }

    @Override
    protected boolean handleCanvasInteraction(double mouseX, double mouseY, int button) {
        Vector2f canvasCoordinates = this.projectToCanvasCoordinates(mouseX, mouseY, this.parentScreen.getMinecraft().getFrameTime());

        if (canvasCoordinates == null) {
            return false; // Can't apply tool if unable to project to canvas coordinates
        }

        this.parentScreen.useTool(canvasCoordinates.x, canvasCoordinates.y);

        return true;
    }

    @Override
    public void tick() {}

    private int getCanvasScale() {
        if (this.parentScreen.getEaselState().canvasMode().equals(PaintingScreen.CanvasMode.OVERLAY)) {
            return this.parentScreen.getEaselState().canvasOverlayState().canvasScale() * 2;
        } else {
            return 2;
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();

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

        /**
         * @todo: [HIGH] This does not account for various game effects,
         * correct solution would be to reflect GameRenderer#getFov() method
         * making it public
         */
        Matrix4f projectionMatrix = minecraft.gameRenderer.getProjectionMatrix(
            minecraft.options.fov().get()
        );

        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(projectionMatrix, VertexSorting.DISTANCE_TO_ORIGIN);

        Matrix4f modelViewMatrix = new Matrix4f(RenderSystem.getModelViewStack().last().pose());
        RenderSystem.getModelViewStack().translate(0.0D, 0.0D, -1000D + net.minecraftforge.client.ForgeHooksClient.getGuiFarPlane());
        RenderSystem.applyModelViewMatrix();

        PoseStack poseStack = guiGraphics.pose();

        Matrix4f lastMatrix = poseStack.last().pose();
        poseStack.popPose();

        poseStack.pushPose();

        Matrix4f canvasViewMatrix = this.getCanvasViewMatrix(partialTick);

        // Camera transformations from identity
        poseStack.last().pose().set(canvasViewMatrix);

        MultiBufferSource.BufferSource renderTypeBufferImpl = guiGraphics.bufferSource();
        CanvasRenderer.getInstance().renderCanvas(poseStack, renderTypeBufferImpl, canvasCode, canvasData, 0xF000F0);
        renderTypeBufferImpl.endBatch();

        this.renderCursor(guiGraphics, mouseX, mouseY, partialTick);

        poseStack.popPose();
        RenderSystem.restoreProjectionMatrix();
        RenderSystem.getModelViewStack().last().pose().set(modelViewMatrix);
        RenderSystem.applyModelViewMatrix();

        poseStack.pushPose();
        poseStack.last().pose().set(lastMatrix);
    }

    private void renderCursor(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        final int CURSOR_SCALE = 6;

        if (mouseX > this.parentScreen.getToolsWindowLeftPos() &&
            mouseX < this.parentScreen.getToolsWindowLeftPos() + this.parentScreen.getToolsWindowWidth() &&
            mouseY > this.parentScreen.getToolsWindowTopPos() &&
            mouseY < this.parentScreen.getToolsWindowTopPos() + this.parentScreen.getToolsWindowHeight()) {
            GLFW.glfwSetInputMode(Minecraft.getInstance().getWindow().getWindow(), GLFW_CURSOR, GLFW_CURSOR_NORMAL);

            // If the cursor is over the tools window, don't render the cursor
            return;
        }

        PoseStack poseStack = guiGraphics.pose();

        if (Zetter.DEBUG_MODE && Zetter.DEBUG_CLIENT) {
            PoseStack.Pose worldTransformations = poseStack.last();
            poseStack.popPose();

            Minecraft minecraft = this.parentScreen.getMinecraft();

            this.drawDebugCanvasNormal(
                guiGraphics,
                this.parentScreen.getCanvasHolderEntity().getPosition(partialTicks).toVector3f(),
                this.parentScreen.getCanvasHolderEntity().getCanvasNormal(),
                minecraft.gameRenderer.getMainCamera().getPosition()
            );

            poseStack.pushPose();
            poseStack.last().pose().set(worldTransformations.pose());
        }

        Tool tool = this.parentScreen.getPaletteState().currentTool();
        AbstractToolParameters toolParameters = this.parentScreen.getToolsParameters().getToolParameters(tool);
        AbstractTool.ToolShape shape = tool.getTool().getShape(toolParameters);

        Vector2f canvasCoordinate = this.projectToCanvasCoordinates(mouseX, mouseY, partialTicks);

        if (canvasCoordinate == null) {
            // If we can't project to canvas, just return
            return;
        }

        poseStack.pushPose();
        poseStack.translate(0.0d, 0.0d, -0.001d);
        poseStack.scale(1.0f / CURSOR_SCALE, 1.0f / CURSOR_SCALE, 1.0f);

        if (shape == null) {
            int radius = 4;

            if (toolParameters instanceof SizeParameterHolder) {
                radius = Math.round(radius * ((SizeParameterHolder) toolParameters).getSize());
            }

            if (radius < 4) {
                radius = 4;
            }

            this.drawCursor(guiGraphics, radius, Mth.floor(canvasCoordinate.x * CURSOR_SCALE), Mth.floor(canvasCoordinate.y * CURSOR_SCALE), CURSOR_SCALE);
        } else {
            this.drawCursor(guiGraphics, shape, Mth.floor(canvasCoordinate.x) * CURSOR_SCALE, Mth.floor(canvasCoordinate.y) * CURSOR_SCALE, CURSOR_SCALE);
        }

        poseStack.popPose();
    }

    private void drawDebugCanvasNormal(GuiGraphics guiGraphics, Vector3f canvasPosition, Vector3f canvasPlaneNormal, Vec3 cameraPosition) {
        Minecraft minecraft = Minecraft.getInstance();
        PoseStack poseStack = guiGraphics.pose();

        poseStack.pushPose();

        VertexConsumer lineBuffer = minecraft.renderBuffers().bufferSource().getBuffer(RenderType.lines());
        Matrix4f matrix4f = poseStack.last().pose();
        Matrix3f matrix3f = poseStack.last().normal();
        Vector3f canvasCameraOrig = new Vector3f(canvasPosition).sub(cameraPosition.toVector3f());
        lineBuffer.vertex(matrix4f, canvasCameraOrig.x, canvasCameraOrig.y, canvasCameraOrig.z).color(0, 0, 255, 255).normal(matrix3f, canvasPlaneNormal.x, canvasPlaneNormal.y, canvasPlaneNormal.z).endVertex();
        Vector3f canvasCameraNormal = new Vector3f(canvasCameraOrig).add(canvasPlaneNormal);
        lineBuffer.vertex(matrix4f, canvasCameraNormal.x, canvasCameraNormal.y, canvasCameraNormal.z).color(0, 0, 255, 255).normal(matrix3f, canvasPlaneNormal.x, canvasPlaneNormal.y, canvasPlaneNormal.z).endVertex();

        poseStack.popPose();
    }

    private @Nullable Vector2f projectToCanvasCoordinates(double mouseX, double mouseY, float partialTicks) {
        Minecraft minecraft = Minecraft.getInstance();
        Camera camera = minecraft.gameRenderer.getMainCamera();

        Matrix4f viewMatrix = new Matrix4f().identity();
        viewMatrix.rotate(Axis.XP.rotationDegrees(camera.getXRot()));
        viewMatrix.rotate(Axis.YP.rotationDegrees(camera.getYRot() + 180.0F));

        Matrix4f projectionMatrix = minecraft.gameRenderer.getProjectionMatrix(
            minecraft.options.fov().get()
        );

        Matrix4f projectionViewMatrix = projectionMatrix.mul(
            viewMatrix,
            new Matrix4f()
        );

        double mx = minecraft.mouseHandler.xpos();
        double my = minecraft.mouseHandler.ypos();

        int width = minecraft.getWindow().getScreenWidth();
        int height = minecraft.getWindow().getScreenHeight();

        Vector3f nearPoint = minecraft.gameRenderer.getMainCamera().getPosition().toVector3f();
        // Flipping Y seems to be necessary because opengl
        Vector3f farPoint = projectionViewMatrix.unproject((float) mx, (float) (height - my), 10f, new int[]{0, 0, width, height}, new Vector3f()).add(nearPoint);
        Vector3f direction = new Vector3f(farPoint.x - nearPoint.x, farPoint.y - nearPoint.y, farPoint.z - nearPoint.z);
        direction.normalize();

        Vector3f canvasHolderPosition = this.parentScreen.getCanvasHolderEntity().getPosition(partialTicks).toVector3f();
        Vector3f canvasPosition = new Vector3f(this.parentScreen.getCanvasHolderEntity().getCanvasOffset());
        canvasPosition.add(canvasHolderPosition);
        Vector3f canvasPlaneNormal = new Vector3f(this.parentScreen.getCanvasHolderEntity().getCanvasNormal());

        float denominator = direction.dot(canvasPlaneNormal);

        if (Mth.abs(denominator) < Mth.EPSILON) {
            return null;
        }

        Vector3f vectorToPlane = new Vector3f(canvasPosition).sub(nearPoint);
        float numerator = vectorToPlane.dot(canvasPlaneNormal);
        float distance = (numerator / denominator);

        Vector3f intersection = new Vector3f(
            nearPoint.x + (direction.x * distance),
            nearPoint.y + (direction.y * distance),
            nearPoint.z + (direction.z * distance)
        );

        Vector3f u = this.parentScreen.getCanvasHolderEntity().getCanvasU();
        Vector3f v = this.parentScreen.getCanvasHolderEntity().getCanvasV();

        Vector3f relativePosition = new Vector3f(intersection).sub(canvasPosition);

        float x = relativePosition.dot(u) * 16.0f + 16.0f;
        float y = relativePosition.dot(v) * 16.0f + 16.0f;

        return new Vector2f(x, y);
    }

    private @Nullable Matrix4f getCanvasViewMatrix(float partialTicks) {
        Minecraft minecraft = Minecraft.getInstance();

        if (!this.parentScreen.getCanvasHolderEntity().hasCanvas()) {
            return null;
        }

        Matrix4f canvasTransform = this.parentScreen.getCanvasHolderEntity().getCanvasMatrixTransform(partialTicks);

        Vec3 entityPosition = this.parentScreen.getCanvasHolderEntity().getPosition(partialTicks);
        Camera camera = minecraft.gameRenderer.getMainCamera();
        Vec3 cameraPosition = camera.getPosition();

        Matrix4f viewMatrix = new Matrix4f().identity();

        // Camera transformations from identity
        viewMatrix.rotate(Axis.XP.rotationDegrees(camera.getXRot()));
        viewMatrix.rotate(Axis.YP.rotationDegrees(camera.getYRot() + 180.0F));

        // World transformations for entity
        viewMatrix.translate((float) (entityPosition.x - cameraPosition.x), (float) (entityPosition.y - cameraPosition.y), (float) (entityPosition.z - cameraPosition.z));
        viewMatrix.rotate(Axis.XP.rotationDegrees(this.parentScreen.getCanvasHolderEntity().getXRot()));
        viewMatrix.rotate(Axis.YP.rotationDegrees(180.0F - this.parentScreen.getCanvasHolderEntity().getYRot()));

        // Canvas transformations
        viewMatrix.mul(canvasTransform);

        return viewMatrix;
    }
}
