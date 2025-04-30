package me.dantaeusb.zetter.client.gui.painting;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
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
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

import java.util.Optional;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL14.GL_FUNC_ADD;
import static org.lwjgl.opengl.GL14.GL_FUNC_SUBTRACT;

/**
 * This is really shit ton of code, handling both rendering
 * and user interactions, probably better decomposed.
 */
public class CanvasLayer implements GuiEventListener, NarratableEntry, Renderable {
  public static final ResourceLocation PAINTING_CHECKER_RESOURCE = new ResourceLocation(Zetter.MOD_ID, "textures/gui/painting/checker.png");

  private final PaintingScreen parentScreen;

  private boolean canvasDragging = false;
  private boolean isHovered = false;

  private double scrollDistance = 0d;
  private long scrollTimestamp = 0;

  protected int leftPos;
  protected int topPos;

  protected int width;
  protected int height;

  public CanvasLayer(PaintingScreen parentScreen) {
    super();

    this.parentScreen = parentScreen;
  }

  public void init(int leftPos, int topPos, int width, int height) {
    this.leftPos = leftPos;
    this.topPos = topPos;
    this.width = width;
    this.height = height;
  }

  private int getCanvasScale() {
    if (this.parentScreen.getEaselState().canvasMode().equals(PaintingScreen.CanvasMode.OVERLAY)) {
      return this.parentScreen.getEaselState().canvasOverlayState().canvasScale();
    } else {
      return 2;
    }
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
      if (this.canvasDragging) {
        this.handleCanvasInteraction(mouseX, mouseY);
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
    return false;
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

      if (this.scrollDistance <= -1.5d && this.canDecreaseCanvasScale()) {
        this.decreaseCanvasScale();
        this.scrollDistance = 0d;
      } else if (this.scrollDistance >= 1.5d && this.canIncreaseCanvasScale()) {
        this.increaseCanvasScale();
        this.scrollDistance = 0d;
      }

      return true;
    }

    return false;
  }

  @Override
  public void setFocused(boolean pFocused) {

  }

  @Override
  public boolean isFocused() {
    return false;
  }

  @Override
  public boolean isMouseOver(double pMouseX, double pMouseY) {
    return pMouseX >= this.leftPos + 4
        && pMouseY >= this.topPos + 4
        && pMouseX < this.leftPos + this.width - 4
        && pMouseY < this.topPos + this.height - 4;
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
    final float canvasX = (float) ((mouseX - this.leftPos - this.parentScreen.getEaselState().canvasOverlayState().canvasOffsetX()) / (float) this.getCanvasScale());
    final float canvasY = (float) ((mouseY - this.topPos - this.parentScreen.getEaselState().canvasOverlayState().canvasOffsetY()) / (float) this.getCanvasScale());

    this.parentScreen.useTool(canvasX, canvasY);

    return true;
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
      if (System.currentTimeMillis() - this.scrollTimestamp > 1500) {
        this.scrollDistance = 0;
      }
    }
  }

  @Override
  public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    if (this.parentScreen.getEaselState().canvasMode().equals(PaintingScreen.CanvasMode.OVERLAY)) {
      this.renderOverlay(guiGraphics, mouseX, mouseY, partialTick);
    } else {
      this.renderImmersive(guiGraphics, mouseX, mouseY, partialTick);
    }
  }

  private void renderOverlay(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
    Minecraft minecraft = this.parentScreen.getMinecraft();

    final int scale = (int) minecraft.getWindow().getGuiScale();
    final int scaleRoundingOffsetX = minecraft.getWindow().getWidth() % scale;
    final int scaleRoundingOffsetY = minecraft.getWindow().getHeight() % scale;

    final CanvasOverlayState canvasOverlayState = this.parentScreen.getEaselState().canvasOverlayState();

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

    this.renderOverlayBorders(guiGraphics, mouseX, mouseY, partialTicks);

    RenderSystem.enableScissor(
        (this.leftPos + 4) * scale - scaleRoundingOffsetX,
        (this.topPos + 4) * scale - scaleRoundingOffsetY,
        (this.width - 8) * scale,
        (this.height - 8) * scale
    );

    this.renderOverlayCheckerboard(guiGraphics, mouseX, mouseY, partialTicks);

    PoseStack poseStack = guiGraphics.pose();
    poseStack.pushPose();
    poseStack.translate(this.topPos + canvasOverlayState.canvasOffsetX(), this.leftPos + canvasOverlayState.canvasOffsetY(), 0.0F);
    poseStack.scale(this.getCanvasScale(), this.getCanvasScale(), 1.0F);

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
        double canvasX = (mouseX - this.leftPos - this.parentScreen.getEaselState().canvasOverlayState().canvasOffsetX()) / (double) this.getCanvasScale();
        double canvasY = (mouseY - this.topPos - this.parentScreen.getEaselState().canvasOverlayState().canvasOffsetY()) / (double) this.getCanvasScale();

        this.renderOverlayCursor(guiGraphics, canvasX, canvasY);
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
  private void renderOverlayBorders(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
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

  private void renderOverlayCheckerboard(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    final CanvasOverlayState canvasOverlayState = this.parentScreen.getEaselState().canvasOverlayState();

    final int offsetX = Math.abs(this.getCanvasScale() * 2 - canvasOverlayState.canvasOffsetX() % (this.getCanvasScale() * 2));
    final int offsetY = Math.abs(this.getCanvasScale() * 2 - canvasOverlayState.canvasOffsetY() % (this.getCanvasScale() * 2));

    final int width = this.width + this.getCanvasScale() * 4;
    final int height = this.height + this.getCanvasScale() * 4;

    float x1 = this.leftPos - offsetX;
    float x2 = x1 + width;
    float y1 = this.topPos - offsetY;
    float y2 = y1 + height;

    // Size of one copy
    float uvScale = this.getCanvasScale() * 2f;

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

  private void renderImmersive(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
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

    Optional<Matrix4f> canvasTransform = this.parentScreen.getCanvasHolderEntity().getCanvasMatrixTransform(partialTick);

    if (canvasTransform.isEmpty()) {
      return;
    }

    Vec3 entityPosition = this.parentScreen.getCanvasHolderEntity().getPosition(partialTick);
    Camera camera = minecraft.gameRenderer.getMainCamera();
    Vec3 cameraPosition = camera.getPosition();

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

    // Camera transformations from identity
    poseStack.last().pose().identity();
    poseStack.mulPose(Axis.XP.rotationDegrees(camera.getXRot()));
    poseStack.mulPose(Axis.YP.rotationDegrees(camera.getYRot() + 180.0F));

    poseStack.pushPose();

    // World transformations for entity
    poseStack.translate(entityPosition.x - cameraPosition.x, entityPosition.y - cameraPosition.y, entityPosition.z - cameraPosition.z);
    poseStack.mulPose(Axis.XP.rotationDegrees(this.parentScreen.getCanvasHolderEntity().getXRot()));
    poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - this.parentScreen.getCanvasHolderEntity().getYRot()));

    // Canvas transformations
    poseStack.mulPoseMatrix(canvasTransform.get());

    MultiBufferSource.BufferSource renderTypeBufferImpl = guiGraphics.bufferSource();
    CanvasRenderer.getInstance().renderCanvas(poseStack, renderTypeBufferImpl, canvasCode, canvasData, 0xF000F0);
    renderTypeBufferImpl.endBatch();

    this.renderImmersiveCursor(guiGraphics, mouseX, mouseY, partialTick);

    poseStack.popPose();
    poseStack.popPose();
    RenderSystem.restoreProjectionMatrix();
    RenderSystem.getModelViewStack().last().pose().set(modelViewMatrix);
    RenderSystem.applyModelViewMatrix();

    poseStack.pushPose();
    poseStack.last().pose().set(lastMatrix);
  }

  private void renderImmersiveCursor(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
    Minecraft minecraft = Minecraft.getInstance();

    // debug
    Camera camera = minecraft.gameRenderer.getMainCamera();
    Vec3 cameraPosition = camera.getPosition();

    PoseStack poseStack = guiGraphics.pose();

    PoseStack.Pose worldTransformations = poseStack.last();
    poseStack.popPose();

    /*
    Matrix4f inverseProjectionMatrix = new Matrix4f(RenderSystem.getProjectionMatrix()).invert();
    Matrix4f inverseProjectionMatrix = new Matrix4f(minecraft.gameRenderer.getProjectionMatrix(
        minecraft.options.fov().get()
    )).invert();
    Matrix4f inverseViewMatrix = new Matrix4f(poseStack.last().pose()).invert();
    */

    double mx = minecraft.mouseHandler.xpos();
    double my = minecraft.mouseHandler.ypos();

    int width = minecraft.getWindow().getScreenWidth();
    int height = minecraft.getWindow().getScreenHeight();

    Matrix4f projectionMatrix = new Matrix4f(RenderSystem.getProjectionMatrix());
    Matrix4f viewMatrix = new Matrix4f(poseStack.last().pose());
    Matrix4f projectionViewMatrix = projectionMatrix.mul(viewMatrix, new Matrix4f());

    Vector3f nearPoint = minecraft.gameRenderer.getMainCamera().getPosition().toVector3f();
    Vector3f farPoint = projectionViewMatrix.unproject((float) mx, (float) my, -1f, new int[]{0, 0, width, height}, new Vector3f()).add(nearPoint);
    Vector3f direction = new Vector3f(farPoint.x - nearPoint.x, farPoint.y - nearPoint.y, farPoint.z - nearPoint.z);
    direction.normalize();

    // Y is wrong, flipped upside down
    Vector3f canvasPosition = new Vector3f(this.parentScreen.getCanvasHolderEntity().getCanvasOffset());
    canvasPosition.add(this.parentScreen.getCanvasHolderEntity().getPosition(partialTicks).toVector3f());
    Vector3f canvasPlaneNormal = new Vector3f(this.parentScreen.getCanvasHolderEntity().getCanvasNormal());
    this.parentScreen.getCanvasHolderEntity().getViewVector(partialTicks);
    //canvasPlaneNormal.rotateY(-((float) Math.toRadians(this.parentScreen.getCanvasHolderEntity().getYRot())));

    float denominator = canvasPlaneNormal.dot(direction);
    if (Mth.abs(denominator) < Mth.EPSILON) {
      // do the right fucking thing
      poseStack.pushPose();
      poseStack.last().pose().set(worldTransformations.pose());
      return;
    }

    float numerator = new Vector3f(canvasPosition.sub(nearPoint)).dot(canvasPlaneNormal);
    float distance = -(numerator / denominator);

    Vector3f intersection = new Vector3f(
        nearPoint.x + (direction.x * distance),
        nearPoint.y + (direction.y * distance),
        nearPoint.z + (direction.z * distance)
    );

    Vector2f canvasCoordinate = new Vector2f(
        intersection.x - canvasPosition.x,
        intersection.y - canvasPosition.y
    );

    //debug
    poseStack.pushPose();

    VertexConsumer lineBuffer = minecraft.renderBuffers().bufferSource().getBuffer(RenderType.lines());
    Matrix4f matrix4f = poseStack.last().pose();
    Matrix3f matrix3f = poseStack.last().normal();
    lineBuffer.vertex(matrix4f, 0, 0, 0).color(0, 0, 255, 255).normal(matrix3f, nearPoint.x, nearPoint.y, nearPoint.z).endVertex();
    lineBuffer.vertex(matrix4f, (float) (intersection.x - cameraPosition.x), (float) (intersection.y - cameraPosition.y), (float) (intersection.z - cameraPosition.z)).color(0, 0, 255, 255).normal(matrix3f, farPoint.x, farPoint.y, farPoint.z).endVertex();

    poseStack.popPose();

    poseStack.pushPose();
    poseStack.last().pose().set(worldTransformations.pose());

    //debug

    /*float x = 2.0f * ((float) mx / (float) width) - 1.0f;
    float y = 1.0f - 2.0f * ((float) my / (float) height);
    x = Mth.clamp(x, -1.0f, 1.0f);
    y = Mth.clamp(y, -1.0f, 1.0f);

    Vector4f screenPosition = new Vector4f(
        x,
        y,
        -1.0f,
        1.0f
    );

    Vector4f canvasPosition = screenPosition.mul(inverseViewMatrix).mul(inverseProjectionMatrix);*/
    /*canvasPosition.w = 1.0f / canvasPosition.w;
    canvasPosition.x *= canvasPosition.w;
    canvasPosition.y *= canvasPosition.w;
    canvasPosition.z *= canvasPosition.w;*/

    Tool tool = this.parentScreen.getPaletteState().currentTool();
    AbstractToolParameters toolParameters = this.parentScreen.getToolsParameters().getToolParameters(tool);
    AbstractTool.ToolShape shape = tool.getTool().getShape(toolParameters);

    poseStack.pushPose();
    poseStack.translate(0.0d, 0.0d, -0.001d);

    if (shape == null) {
      int radius = this.getCanvasScale() * 2;

      if (toolParameters instanceof SizeParameterHolder) {
        radius = Math.round(this.getCanvasScale() * ((SizeParameterHolder) toolParameters).getSize());
      }

      if (radius < 4) {
        radius = 4;
      }

      this.renderCursor(guiGraphics, radius, (int) canvasCoordinate.x, (int) canvasCoordinate.y);
    } else {
      this.renderCursor(guiGraphics, shape, (int) canvasCoordinate.x, (int) canvasCoordinate.y);
    }

    poseStack.popPose();
  }

  private void renderOverlayCursor(GuiGraphics guiGraphics, double canvasX, double canvasY) {
    Tool tool = this.parentScreen.getPaletteState().currentTool();
    AbstractToolParameters toolParameters = this.parentScreen.getToolsParameters().getToolParameters(tool);
    AbstractTool.ToolShape shape = tool.getTool().getShape(toolParameters);

    if (shape == null) {
      int radius = this.getCanvasScale() * 2;

      if (toolParameters instanceof SizeParameterHolder) {
        radius = Math.round(this.getCanvasScale() * ((SizeParameterHolder) toolParameters).getSize());
      }

      if (radius < 4) {
        radius = 4;
      }

      int globalX = this.leftPos + this.parentScreen.getEaselState().canvasOverlayState().canvasOffsetX() + (int) Math.floor(canvasX * this.getCanvasScale()) - 1;
      int globalY = this.topPos + this.parentScreen.getEaselState().canvasOverlayState().canvasOffsetY() + (int) Math.floor(canvasY * this.getCanvasScale()) - 1;

      this.renderCursor(guiGraphics, radius, globalX, globalY);
    } else {
      // Offset for the current shape, in canvas pixels
      final int offset = -(shape.getSize() / 2);

      int globalX = this.leftPos + this.parentScreen.getEaselState().canvasOverlayState().canvasOffsetX() + (int) Math.floor(canvasX) * this.getCanvasScale();
      int globalY = this.topPos + this.parentScreen.getEaselState().canvasOverlayState().canvasOffsetY() + (int) Math.floor(canvasY) * this.getCanvasScale();

      this.renderCursor(guiGraphics, shape, globalX, globalY);
    }
    /*this.hLine(matrixStack, globalX1, globalX2, globalY1, 0x80808080);
    this.hLine(matrixStack, globalX1, globalX2, globalY2, 0x80808080);
    this.vLine(matrixStack, globalX1, globalY1, globalY2, 0x80808080);
    this.vLine(matrixStack, globalX2, globalY1, globalY2, 0x80808080);*/
  }

  private void renderCursor(GuiGraphics guiGraphics, int radius, int offsetX, int offsetY) {
    RenderSystem.blendEquation(GL_FUNC_SUBTRACT);

    //GLFW.glfwSetInputMode(Minecraft.getInstance().getWindow().getWindow(), GLFW_CURSOR, GLFW_CURSOR_HIDDEN);

    guiGraphics.hLine(offsetX - radius + 1, offsetX - 2, offsetY, 0x80808080);
    guiGraphics.hLine(offsetX + radius - 1, offsetX + 2, offsetY, 0x80808080);

    guiGraphics.vLine(offsetX, offsetY - radius, offsetY - 1, 0x80808080);
    guiGraphics.vLine(offsetX, offsetY + radius, offsetY + 1, 0x80808080);

    RenderSystem.blendEquation(GL_FUNC_ADD);
  }

  private void renderCursor(GuiGraphics guiGraphics, AbstractTool.ToolShape shape, int offsetX, int offsetY) {
    RenderSystem.blendEquation(GL_FUNC_SUBTRACT);

    //GLFW.glfwSetInputMode(Minecraft.getInstance().getWindow().getWindow(), GLFW_CURSOR, GLFW_CURSOR_NORMAL);

    for (AbstractTool.ShapeLine line : shape.getLines()) {
      // Relative positions from the cursor "center" in canvas pixels
      // Center is
      int posX = (line.posX()) * this.getCanvasScale();
      int posY = (line.posY()) * this.getCanvasScale();
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

        final int minX = offsetX + posX;
        guiGraphics.hLine(minX, minX + length, offsetY + posY, 0x80808080);
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

        final int minY = offsetY + posY;
        guiGraphics.vLine(offsetX + posX, minY, minY + length, 0x80808080);
      }
    }

    RenderSystem.blendEquation(GL_FUNC_ADD);
  }

  @Override
  public NarrationPriority narrationPriority() {
    if (this.isFocused()) {
      return NarratableEntry.NarrationPriority.FOCUSED;
    } else {
      return this.isHovered ? NarratableEntry.NarrationPriority.HOVERED : NarratableEntry.NarrationPriority.NONE;
    }
  }

  @Override
  public void updateNarration(NarrationElementOutput narrationElementOutput) {

  }
}
