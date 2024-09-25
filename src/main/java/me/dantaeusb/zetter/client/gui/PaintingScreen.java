package me.dantaeusb.zetter.client.gui;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.capability.canvastracker.CanvasTracker;
import me.dantaeusb.zetter.client.gui.easel.*;
import me.dantaeusb.zetter.client.renderer.CanvasRenderer;
import me.dantaeusb.zetter.core.Helper;
import me.dantaeusb.zetter.core.ZetterRenderTypes;
import me.dantaeusb.zetter.entity.item.CanvasHolderEntity;
import me.dantaeusb.zetter.storage.AbstractCanvasData;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

import java.util.List;
import java.util.Optional;

public class PaintingScreen extends Screen {
  // This is the resource location for the background image
  public static final ResourceLocation PAINTING_GUI_TEXTURE_RESOURCE = new ResourceLocation(Zetter.MOD_ID, "textures/gui/painting.png");
  private static final float MAX_ASPECT_RATIO = 16.0f / 10.0f;

  private final List<AbstractEaselWidget> paintingWidgets = Lists.newArrayList();

  private CanvasHolderEntity canvasHolderEntity;

  /**
   * Starting X position for the Gui. Inconsistent use for Gui backgrounds.
   */
  protected int leftPos;
  /**
   * Starting Y position for the Gui. Inconsistent use for Gui backgrounds.
   */
  protected int topPos;

  /**
   * The X size of the inventory window in pixels.
   */
  protected int imageWidth = 187;
  /**
   * The Y size of the inventory window in pixels.
   */
  protected int imageHeight = 288;

  /**
   * @todo: Use player's last known preference
   */
  private CanvasMode canvasMode = CanvasMode.IMMERSIVE_BACKGROUND;

  public PaintingScreen(CanvasHolderEntity canvasHolderEntity) {
    super(Component.translatable("screen.zetter.painting"));

    this.canvasHolderEntity = canvasHolderEntity;
  }

  @Override
  protected void init() {
    super.init();

    float aspectRatio = (float) this.width / (float) this.height;
    int virtualWidth = this.width;
    int virtualHeight = this.height;

    if (aspectRatio > MAX_ASPECT_RATIO) {
      virtualWidth = (int) (this.height * MAX_ASPECT_RATIO);
    } else {
      virtualHeight = (int) (this.width / MAX_ASPECT_RATIO);
    }

    this.leftPos = (this.width - virtualWidth) / 2 + virtualWidth - this.imageWidth - 10;
    this.topPos = (this.height - virtualHeight) / 2 + (virtualHeight - this.imageHeight) / 2;
  }

  @Override
  public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
    this.renderBackground(guiGraphics);
    this.renderBg(guiGraphics, partialTicks, mouseX, mouseY);
    RenderSystem.disableDepthTest();
    //net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.client.event.ContainerScreenEvent.Render.Background(this, guiGraphics, mouseX, mouseY));
    super.render(guiGraphics, mouseX, mouseY, partialTicks);
    guiGraphics.pose().pushPose();
    guiGraphics.pose().translate((float) this.leftPos, (float) this.topPos, 0.0F);
    super.render(guiGraphics, mouseX, mouseY, partialTicks);

    this.renderPicker(guiGraphics, partialTicks);
    this.renderCanvas(guiGraphics, partialTicks);
    //this.renderTooltip(guiGraphics, mouseX, mouseY);
    RenderSystem.enableDepthTest();
  }

  /**
   * Overriding renderBackground to draw our own background optionally
   *
   * @param guiGraphics
   */
  public void renderBackground(@NotNull GuiGraphics guiGraphics) {
    switch (this.canvasMode) {
      case IMMERSIVE:
        break;
      case IMMERSIVE_BACKGROUND:
      case OVERLAY:
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0f, 0f, -1000f);
        guiGraphics.fillGradient(0, 0, this.width, this.height, 0xC0101010, 0xD0101010);
        guiGraphics.pose().popPose();
        break;
    }

    net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.client.event.ScreenEvent.BackgroundRendered(this, guiGraphics));
  }

  protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
    RenderSystem.setShader(GameRenderer::getPositionTexShader);
    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    RenderSystem.setShaderTexture(0, PAINTING_GUI_TEXTURE_RESOURCE);

    guiGraphics.blit(PAINTING_GUI_TEXTURE_RESOURCE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);
  }

  protected void renderCanvas(GuiGraphics guiGraphics, float partialTicks) {
    Minecraft minecraft = Minecraft.getInstance();

    if (minecraft.level == null) {
      return;
    }

    String canvasCode = this.canvasHolderEntity.getCanvasCode();
    if (canvasCode == null) {
      return;
    }

    CanvasTracker canvasTracker = Helper.getLevelCanvasTracker(minecraft.level);
    AbstractCanvasData canvasData = canvasTracker.getCanvasData(canvasCode);

    if (canvasData == null) {
      return;
    }

    Optional<Matrix4f> matrixTransform = this.canvasHolderEntity.getCanvasMatrixTransform(partialTicks);

    if (matrixTransform.isEmpty()) {
      return;
    }

    guiGraphics.flush();

    Vec3 entityPosition = this.canvasHolderEntity.getPosition(partialTicks);
    Camera camera = minecraft.gameRenderer.getMainCamera();

    Matrix4f projectionMatrix = minecraft.gameRenderer.getProjectionMatrix(70.0F);

    RenderSystem.backupProjectionMatrix();
    RenderSystem.setProjectionMatrix(projectionMatrix, VertexSorting.DISTANCE_TO_ORIGIN);

    RenderSystem.getModelViewStack().translate(0.0D, 0.0D, -1000F + net.minecraftforge.client.ForgeHooksClient.getGuiFarPlane());
    RenderSystem.applyModelViewMatrix();

    Vec3 cameraPosition = camera.getPosition();

    PoseStack poseStack = guiGraphics.pose();

    Matrix4f lastMatrix = poseStack.last().pose();
    poseStack.popPose();

    poseStack.pushPose();
    poseStack.last().pose().identity();

    poseStack.mulPose(Axis.XP.rotationDegrees(camera.getXRot()));
    poseStack.mulPose(Axis.YP.rotationDegrees(camera.getYRot() + 180.0F));

    poseStack.translate(entityPosition.x - cameraPosition.x, entityPosition.y - cameraPosition.y, entityPosition.z - cameraPosition.z);
    poseStack.mulPose(Axis.XP.rotationDegrees(this.canvasHolderEntity.getXRot()));
    poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - this.canvasHolderEntity.getYRot()));
    poseStack.mulPoseMatrix(matrixTransform.get());

    MultiBufferSource.BufferSource renderTypeBufferImpl = guiGraphics.bufferSource();
    CanvasRenderer.getInstance().renderCanvas(poseStack, renderTypeBufferImpl, canvasCode, canvasData, 0xF000F0);
    renderTypeBufferImpl.endBatch();

    poseStack.popPose();
    RenderSystem.restoreProjectionMatrix();
    RenderSystem.getModelViewStack().setIdentity();
    RenderSystem.applyModelViewMatrix();

    poseStack.pushPose();
    poseStack.last().pose().set(lastMatrix);
  }

  public void renderPicker(GuiGraphics guiGraphics, float partialTicks) {
    RenderSystem.setShader(ZetterRenderTypes::getOklchPaletteShader);
    ZetterRenderTypes.setOklchLightness(0.5f);

    Matrix4f matrix4f = guiGraphics.pose().last().pose();

    int pX1 = 62;
    int pY1 = 16;
    int pX2 = pX1 + 116;
    int pY2 = pY1 + 116;

    BufferBuilder bufferbuilder = Tesselator.getInstance().getBuilder();
    bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
    bufferbuilder.vertex(matrix4f, (float)pX1, (float)pY1, 0.0f).uv(0, 0).endVertex();
    bufferbuilder.vertex(matrix4f, (float)pX1, (float)pY2, 0.0f).uv(0, 1).endVertex();
    bufferbuilder.vertex(matrix4f, (float)pX2, (float)pY2, 0.0f).uv(1, 1).endVertex();
    bufferbuilder.vertex(matrix4f, (float)pX2, (float)pY1, 0.0f).uv(1, 0).endVertex();
    BufferUploader.drawWithShader(bufferbuilder.end());
  }

  /**
   * Cancel closing screen when pressing "E", handle input properly
   *
   * @param keyCode
   * @param scanCode
   * @param modifiers
   * @return
   */
  @Override
  public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
    /*if (this.getCurrentTab().keyPressed(keyCode, scanCode, modifiers)) {
      return true;
    }

    switch (keyCode) {
      case GLFW.GLFW_KEY_ESCAPE:
        assert this.minecraft != null;
        assert this.minecraft.player != null;
        this.minecraft.player.closeContainer();
        return true;
      case Pencil.HOTKEY:
        this.getMenu().setCurrentTool(Tools.PENCIL);
        return true;
      case Brush.HOTKEY:
        this.getMenu().setCurrentTool(Tools.BRUSH);
        return true;
      case Eyedropper.HOTKEY:
        this.getMenu().setCurrentTool(Tools.EYEDROPPER);
        return true;
      case Eyedropper.QUICK_TOOL_KEY:
        this.activateQuickTool(Tools.EYEDROPPER);
        return true;
      case Bucket.HOTKEY:
        this.getMenu().setCurrentTool(Tools.BUCKET);
        return true;
      case Hand.HOTKEY:
        this.getMenu().setCurrentTool(Tools.HAND);
        return true;
      case Hand.QUICK_TOOL_KEY:
        this.activateQuickTool(Tools.HAND);
        return true;
      case PaletteWidget.SWAP_HOTKEY: {
        final int row = (this.getMenu().getCurrentPaletteSlot() / 2) * 2;
        final int offset = this.getMenu().getCurrentPaletteSlot() % 2 == 0 ? 1 : 0;
        this.getMenu().setCurrentPaletteSlot(row + offset);
        return true;
      }
      case GLFW.GLFW_KEY_UP: {
        final int row = this.getMenu().getCurrentPaletteSlot() / 2;
        final int offset = this.getMenu().getCurrentPaletteSlot() % 2;

        if (row <= 0) {
          return false;
        }

        this.getMenu().setCurrentPaletteSlot((row - 1) * 2 + offset);

        return true;
      }
      case ZoomWidget.ZOOM_OUT_HOTKEY:
        this.getMenu().decreaseCanvasScale();
        return true;
      case ZoomWidget.ZOOM_IN_HOTKEY:
        this.getMenu().increaseCanvasScale();
        return true;
      case GLFW.GLFW_KEY_DOWN: {
        final int row = this.getMenu().getCurrentPaletteSlot() / 2;
        final int offset = this.getMenu().getCurrentPaletteSlot() % 2;

        if (row >= 7) {
          return false;
        }

        this.getMenu().setCurrentPaletteSlot((row + 1) * 2 + offset);

        return true;
      }
      case HistoryWidget.UNDO_HOTKEY:
        if (Screen.hasControlDown()) {
          this.getMenu().undo();
          return true;
        }
      case HistoryWidget.REDO_HOTKEY:
        if (Screen.hasControlDown()) {
          this.getMenu().redo();
          return true;
        }
    }*/

    return super.keyPressed(keyCode, scanCode, modifiers);
  }

  /**
   * Handle quick tool key release
   *
   * @param keyCode
   * @param scanCode
   * @param modifiers
   * @return
   */
  @Override
  public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
    /*if (this.getCurrentTab().keyReleased(keyCode, scanCode, modifiers)) {
      return true;
    }

    switch (keyCode) {
      case Hand.QUICK_TOOL_KEY:
      case Eyedropper.QUICK_TOOL_KEY:
        this.deactivateQuickTool();
        return true;
    }*/

    return super.keyReleased(keyCode, scanCode, modifiers);
  }

  @Override
  public boolean charTyped(char codePoint, int modifiers) {
    //this.getCurrentTab().charTyped(codePoint, modifiers);

    return super.charTyped(codePoint, modifiers);
  }

  /**
   * We have a little complicated logic with active tabs here
   *
   * @param mouseX
   * @param mouseY
   * @param button
   * @return
   */
  @Override
  public boolean mouseClicked(double mouseX, double mouseY, int button) {
    /*if (this.dragStart == null) {
      this.dragStart = new double[]{mouseX, mouseY};
      this.dragStartCanvasOffset = new int[]{this.getMenu().getCanvasOffsetX(), this.getMenu().getCanvasOffsetY()};
    }

    // We need to handle tab clicks first as we might have input fields
    // Like color HEX that don't update when focused
    // So palette change will be ignored if we handle it first
    this.getCurrentTab().mouseClicked(mouseX, mouseY, button);*/

    return super.mouseClicked(mouseX, mouseY, button);
  }

  /**
   * Unfortunately this event is not passed to children
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
    /*this.dragCurrent = new double[]{mouseX, mouseY};

    this.canvasWidget.mouseDragged(mouseX, mouseY, button, dragX, dragY);

    this.getCurrentTab().mouseDragged(mouseX, mouseY, button, dragX, dragY);*/

    return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
  }

  /**
   * This one not passed when out of widget bounds but we need to track this event to release slider/pencil
   *
   * @param mouseX
   * @param mouseY
   * @param button
   * @return
   */
  @Override
  public boolean mouseReleased(double mouseX, double mouseY, int button) {
    /*this.canvasWidget.mouseReleased(mouseX, mouseY, button);

    this.getCurrentTab().mouseReleased(mouseX, mouseY, button);

    // Reset dragging
    if (this.dragStart != null || this.dragCurrent != null || this.dragStartCanvasOffset != null) {
      this.dragStart = null;
      this.dragStartCanvasOffset = null;
      this.dragCurrent = null;
    }*/

    return super.mouseReleased(mouseX, mouseY, button);
  }

  @Override
  public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
    /*if (hasControlDown()) {
      if (this.getMenu().getCurrentTool() == Tools.BRUSH) {
        AbstractToolParameters parameters = this.getMenu().getCurrentToolParameters();

        if (parameters instanceof SizeParameterHolder) {
          float newSize = ((SizeParameterHolder) parameters).getSize() + (float) delta;
          newSize = Math.min(Math.max(newSize, BrushParameters.MIN_SIZE), BrushParameters.MAX_SIZE);

          ((SizeParameterHolder) parameters).setSize(newSize);
          return true;
        }
      } else if (this.getMenu().getCurrentTool() == Tools.PENCIL) {
        AbstractToolParameters parameters = this.getMenu().getCurrentToolParameters();

        if (parameters instanceof SizeParameterHolder) {
          float newSize = ((SizeParameterHolder) parameters).getSize() + (delta > 0 ? 1 : -1);
          newSize = Math.min(Math.max(newSize, PencilParameters.MIN_SIZE), PencilParameters.MAX_SIZE);

          ((SizeParameterHolder) parameters).setSize(newSize);
          return true;
        }
      }
    }

    if (this.canvasWidget.mouseScrolled(mouseX, mouseY, delta)) {
      return true;
    }*/

    return false;
  }

  /*
   * Helpers
   */

  /**
   * @param x
   * @param y
   * @param xSize
   * @param ySize
   * @param mouseX
   * @param mouseY
   * @return
   * @todo: [LOW] Use this.isPointInRegion
   */
  // Returns true if the given x,y coordinates are within the given rectangle
  public static boolean isInRect(int x, int y, int xSize, int ySize, final int mouseX, final int mouseY) {
    return ((mouseX >= x && mouseX <= x + xSize) && (mouseY >= y && mouseY <= y + ySize));
  }

  public enum CanvasMode {
    IMMERSIVE,
    IMMERSIVE_BACKGROUND,
    OVERLAY
  }
}
