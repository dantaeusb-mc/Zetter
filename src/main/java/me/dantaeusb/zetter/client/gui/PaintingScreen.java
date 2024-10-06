package me.dantaeusb.zetter.client.gui;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.client.gui.painting.*;
import me.dantaeusb.zetter.client.gui.painting.tool.AbstractToolTabGroupWidget;
import me.dantaeusb.zetter.client.gui.painting.tool.BrushTabGroupWidget;
import me.dantaeusb.zetter.client.gui.painting.tool.BucketTabGroupWidget;
import me.dantaeusb.zetter.client.gui.painting.tool.PencilTabGroupWidget;
import me.dantaeusb.zetter.client.gui.painting.util.state.CanvasOverlayState;
import me.dantaeusb.zetter.client.gui.painting.util.state.PaintingScreenState;
import me.dantaeusb.zetter.client.gui.painting.util.PaletteAccessor;
import me.dantaeusb.zetter.client.gui.painting.util.state.ToolsParameters;
import me.dantaeusb.zetter.core.tools.Color;
import me.dantaeusb.zetter.entity.item.CanvasHolderEntity;
import me.dantaeusb.zetter.painting.Tools;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @todo: Close when canvas is destroyed / removed
 */
public class PaintingScreen extends Screen {
  // This is the resource location for the background image
  public static final ResourceLocation PAINTING_GUI_TEXTURE_RESOURCE = new ResourceLocation(Zetter.MOD_ID, "textures/gui/painting.png");
  private static final float MAX_ASPECT_RATIO = 16.0f / 10.0f;

  private final List<AbstractPaintingWidget> paintingWidgets = Lists.newArrayList();
  private final List<AbstractToolTabGroupWidget> toolWidgets = Lists.newArrayList();

  private final CanvasHolderEntity canvasHolderEntity;

  protected int virtualWindowWidth;
  protected int virtualWindowHeight;
  protected int canvasWindowLeftPos;
  protected int canvasWindowTopPos;
  protected int toolsWindowLeftPos;
  protected int toolsWindowTopPos;

  /**
   * The X size of the inventory window in pixels.
   */
  protected int imageWidth = 195;
  /**
   * The Y size of the inventory window in pixels.
   */
  protected int imageHeight = 256;

  private final PaletteAccessor paletteAccessor;

  private PaintingScreenState paintingScreenState;

  private ToolsParameters toolsParameters;

  // Widgets
  private CanvasLayer canvasLayer;
  private ToolsWidget toolsWidget;
  private ZoomWidget zoomWidget;
  private CanvasModeWidget canvasModeWidget;
  private ColorPickerWidget colorPickerWidget;
  private BrushTabGroupWidget brushToolWidget;
  private PencilTabGroupWidget pencilToolWidget;
  private BucketTabGroupWidget bucketToolWidget;

  public PaintingScreen(ItemStack paletteStack, CanvasHolderEntity canvasHolderEntity) {
    super(Component.translatable("screen.zetter.painting"));

    this.paletteAccessor = new PaletteAccessor(paletteStack);
    this.canvasHolderEntity = canvasHolderEntity;

    // @todo: Read from user capability or something
    this.paintingScreenState = new PaintingScreenState(
        this.paletteAccessor.getPaletteColor(0),
        0,
        Tools.PENCIL,
        CanvasMode.IMMERSIVE_BACKGROUND,
        ColorSpace.okHSL,
        new CanvasOverlayState(0, 0, 1)
    );
  }

  @Override
  protected void init() {
    super.init();

    float aspectRatio = (float) this.width / (float) this.height;
    this.virtualWindowWidth = this.width;
    this.virtualWindowHeight = this.height;

    if (aspectRatio > MAX_ASPECT_RATIO) {
      this.virtualWindowWidth = (int) (this.height * MAX_ASPECT_RATIO);
    } else {
      this.virtualWindowHeight = (int) (this.width / MAX_ASPECT_RATIO);
    }

    this.canvasWindowLeftPos = (this.width - this.virtualWindowWidth) / 2 + 10;
    this.canvasWindowTopPos = (this.height - this.virtualWindowHeight) / 2;

    this.toolsWindowLeftPos = (this.width - this.virtualWindowWidth) / 2 + this.virtualWindowWidth - this.imageWidth - 10;
    this.toolsWindowTopPos = (this.height - this.virtualWindowHeight) / 2 + (this.virtualWindowHeight - this.imageHeight) / 2;

    final int TOOLS_WIDGET_POSITION_X = 4;
    final int TOOLS_WIDGET_POSITION_Y = 16;
    final int ZOOM_WIDGET_POSITION_X = 4;
    final int ZOOM_WIDGET_POSITION_Y = 205;
    final int CANVAS_MODE_WIDGET_POSITION_X = 4;
    final int CANVAS_MODE_WIDGET_POSITION_Y = 152;
    final int COLOR_PICKER_WIDGET_POSITION_X = 31;
    final int COLOR_PICKER_WIDGET_POSITION_Y = 0;

    this.canvasLayer = new CanvasLayer(this);
    this.addWidget(this.canvasLayer);

    this.canvasLayer.init(this.canvasWindowLeftPos, this.canvasWindowTopPos, this.virtualWindowWidth - this.imageWidth - 10, this.virtualWindowHeight);

    this.toolsWidget = new ToolsWidget(this, TOOLS_WIDGET_POSITION_X, TOOLS_WIDGET_POSITION_Y);
    this.addPaintingWidget(this.toolsWidget);
    this.zoomWidget = new ZoomWidget(this, ZOOM_WIDGET_POSITION_X, ZOOM_WIDGET_POSITION_Y);
    this.addPaintingWidget(this.zoomWidget);
    this.canvasModeWidget = new CanvasModeWidget(this, CANVAS_MODE_WIDGET_POSITION_X, CANVAS_MODE_WIDGET_POSITION_Y);
    this.addPaintingWidget(this.canvasModeWidget);

    this.colorPickerWidget = new ColorPickerWidget(this, COLOR_PICKER_WIDGET_POSITION_X, COLOR_PICKER_WIDGET_POSITION_Y);
    this.addPaintingWidget(this.colorPickerWidget);

    this.brushToolWidget = new BrushTabGroupWidget(this);
    this.addToolWidget(this.brushToolWidget);
    this.pencilToolWidget = new PencilTabGroupWidget(this);
    this.addToolWidget(this.pencilToolWidget);
    this.bucketToolWidget = new BucketTabGroupWidget(this);
    this.addToolWidget(this.bucketToolWidget);

    this.setPaintingScreenState(this.paintingScreenState);
  }

  public void addPaintingWidget(AbstractPaintingWidget paintingWidget) {
    this.addRenderableWidget(paintingWidget);
    this.paintingWidgets.add(paintingWidget);
  }

  private void addToolWidget(AbstractToolTabGroupWidget paintingWidget) {
    this.addRenderableWidget(paintingWidget);
    this.paintingWidgets.add(paintingWidget);
    this.toolWidgets.add(paintingWidget);
  }

  /*
   * Interactions
   */
  public PaintingScreenState getPaintingScreenState() {
    return this.paintingScreenState;
  }

  public void setPaintingScreenState(PaintingScreenState paintingScreenState) {
    paintingScreenState = this.beforePaintingScreenStateChange(paintingScreenState);
    this.paintingScreenState = paintingScreenState;
    this.afterPaintingScreenStateChange(paintingScreenState);
  }

  public ToolsParameters getToolsParameters() {
    return this.toolsParameters;
  }

  public Color getPaletteColor(int index) {
    return this.paletteAccessor.getPaletteColor(index);
  }

  private PaintingScreenState beforePaintingScreenStateChange(PaintingScreenState newState) {
    if (newState.currentPaletteSlot() != this.paintingScreenState.currentPaletteSlot()) {
      newState = newState.withCurrentColor(this.paletteAccessor.getPaletteColor(newState.currentPaletteSlot()));
    }

    if (newState.currentColor().getHsl() != this.paintingScreenState.currentColor().getHsl()) {
      this.paletteAccessor.setPaletteColor(this.paintingScreenState.currentPaletteSlot(), newState.currentColor());
    }

    return newState;
  }

  private void afterPaintingScreenStateChange(PaintingScreenState newState) {
    for (AbstractToolTabGroupWidget toolWidget : this.toolWidgets) {
      toolWidget.setVisibility(newState.currentTool().equals(toolWidget.getTool()));
    }
  }

  public CanvasHolderEntity getCanvasHolderEntity() {
    return this.canvasHolderEntity;
  }

  /*
   * History
   */

  public boolean canUndo() {
    return false;
  }

  public void undo() {
    // @todo: Implement undo
  }

  public boolean canRedo() {
    return false;
  }

  public void redo() {
    // @todo: Implement undo
  }

  /*
   * Render
   */

  @Override
  public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
    this.renderBackground(guiGraphics);
    this.renderBg(guiGraphics, partialTicks, mouseX, mouseY);
    //net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.client.event.ContainerScreenEvent.Render.Background(this, guiGraphics, mouseX, mouseY));
    guiGraphics.pose().pushPose();
    guiGraphics.pose().translate((float) this.toolsWindowLeftPos, (float) this.toolsWindowTopPos, 0.0F);
    RenderSystem.disableDepthTest();

    super.render(guiGraphics, mouseX, mouseY, partialTicks);

    //this.renderTooltip(guiGraphics, mouseX, mouseY);
    RenderSystem.enableDepthTest();

    guiGraphics.pose().popPose();
  }

  /**
   * Overriding renderBackground to draw our own background optionally
   *
   * @param guiGraphics
   */
  public void renderBackground(@NotNull GuiGraphics guiGraphics) {
    switch (this.getPaintingScreenState().canvasMode()) {
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

    guiGraphics.blit(PAINTING_GUI_TEXTURE_RESOURCE, this.toolsWindowLeftPos, this.toolsWindowTopPos, 0, 0, this.imageWidth, this.imageHeight);
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
    for (AbstractPaintingWidget paintingWidget : this.paintingWidgets) {
      if (paintingWidget.charTyped(codePoint, modifiers)) {
        return true;
      }
    }

    return super.charTyped(codePoint, modifiers);
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
    for (AbstractPaintingWidget paintingWidget : this.paintingWidgets) {
      if (paintingWidget.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
        return true;
      }
    }

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
    for (AbstractPaintingWidget paintingWidget : this.paintingWidgets) {
      if (paintingWidget.mouseScrolled(mouseX, mouseY, delta)) {
        return true;
      }
    }

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

  /**
   * Expose some methods for widgets
   */

  public Font getFont() {
    return this.font;
  }

  /*
   * Helpers
   */

  public enum ColorSpace {
    HSL("hsl"),
    okHSL("okhsl");

    public final String code;

    ColorSpace(String code) {
      this.code = code;
    }
  }

  public enum CanvasMode {
    IMMERSIVE,
    IMMERSIVE_BACKGROUND,
    OVERLAY
  }
}
