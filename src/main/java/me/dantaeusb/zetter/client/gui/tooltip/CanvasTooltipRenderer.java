package me.dantaeusb.zetter.client.gui.tooltip;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.capability.canvastracker.CanvasTracker;
import me.dantaeusb.zetter.client.renderer.CanvasRenderer;
import me.dantaeusb.zetter.core.Helper;
import me.dantaeusb.zetter.core.ZetterItems;
import me.dantaeusb.zetter.item.CanvasItem;
import me.dantaeusb.zetter.item.PaintingItem;
import me.dantaeusb.zetter.storage.AbstractCanvasData;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Timer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

public class CanvasTooltipRenderer implements ClientTooltipComponent {
  private static final ResourceLocation CANVAS_PENDING_BORDER_TEXTURE_RESOURCE = new ResourceLocation(Zetter.MOD_ID, "textures/gui/tooltip/canvas_pending_border.png");
  private static final ResourceLocation CANVAS_PENDING_LOADER_TEXTURE_RESOURCE = new ResourceLocation(Zetter.MOD_ID, "textures/gui/tooltip/canvas_pending_loader.png");

  private final CanvasComponent component;
  private final Timer loaderTimer = new Timer(3.0F, 0L);
  private int loaderFrame = 0;

  public CanvasTooltipRenderer(CanvasComponent component) {
    this.component = component;
  }

  public void tick() {

  }

  @Override
  public void renderImage(Font font, int tooltipX, int tooltipY, GuiGraphics guiGraphics) {
    Minecraft mc = Minecraft.getInstance();

    ItemStack stack = this.component.stack();
    String canvasCode = getCanvasCode(stack);

    if (mc.level == null || getCanvasCode(stack) == null) {
      return;
    }

    CanvasTracker canvasTracker = Helper.getLevelCanvasTracker(mc.level);
    AbstractCanvasData canvasData = canvasTracker.getCanvasData(canvasCode);

    if (canvasData == null) {
      CanvasRenderer.getInstance().queueCanvasTextureUpdate(canvasCode);

      this.renderLoader(font, tooltipX, tooltipY, guiGraphics);
      return;
    }

    this.renderCanvas(font, tooltipX, tooltipY, guiGraphics, canvasCode, canvasData);
  }

  private void renderCanvas(Font font, int tooltipX, int tooltipY, GuiGraphics guiGraphics, String canvasCode, AbstractCanvasData canvasData) {
    Minecraft mc = Minecraft.getInstance();
    PoseStack pose = guiGraphics.pose();

    float scale = CanvasTooltipRenderer.getScale(canvasData.getWidth(), canvasData.getHeight());

    RenderSystem.setShader(GameRenderer::getPositionTexShader);
    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

    pose.pushPose();
    pose.translate(tooltipX, tooltipY, 500);
    pose.scale(scale, scale, 1);
    RenderSystem.enableBlend();

    pose.translate(0, 0, 1);
    CanvasRenderer.getInstance().renderCanvas(
        pose,
        guiGraphics.bufferSource(),
        canvasCode,
        canvasData,
        0xF000F0
    );

    pose.popPose();
  }

  private void renderLoader(Font font, int tooltipX, int tooltipY, GuiGraphics guiGraphics) {
    PoseStack pose = guiGraphics.pose();

    final int[] size = getCanvasSize(this.component.stack);
    final int width = size[0];
    final int height = size[1];
    final float scale = getScale(width, height);

    final int scaledWidth = (int) (width * scale);
    final int scaledHeight = (int) (height * scale);

    RenderSystem.setShader(GameRenderer::getPositionTexShader);
    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

    pose.pushPose();
    pose.translate(tooltipX, tooltipY, 500);
    pose.scale(1, 1, 1);
    RenderSystem.enableBlend();

    // Nine sliced doesn't work for non 256x256 textures
    //guiGraphics.blitNineSliced(CANVAS_PENDING_BORDER_TEXTURE_RESOURCE, 0, 0, scaledWidth, scaledHeight, 2, 0, 0, 0, 0, 7, 7);

    // Top left
    guiGraphics.blit(CANVAS_PENDING_BORDER_TEXTURE_RESOURCE, 0, 0, 3, 3, 0, 0, 3, 3, 7, 7);
    // Top
    guiGraphics.blit(CANVAS_PENDING_BORDER_TEXTURE_RESOURCE, 3, 0, scaledWidth - 6, 3, 3, 0, 1, 3, 7, 7);
    // Top right
    guiGraphics.blit(CANVAS_PENDING_BORDER_TEXTURE_RESOURCE, scaledWidth - 3, 0, 3, 3, 4, 0, 3, 3, 7, 7);
    // Right
    guiGraphics.blit(CANVAS_PENDING_BORDER_TEXTURE_RESOURCE, scaledWidth - 3, 3, 3, scaledHeight - 6, 4, 3, 3, 1, 7, 7);
    // Bottom right
    guiGraphics.blit(CANVAS_PENDING_BORDER_TEXTURE_RESOURCE, scaledWidth - 3, scaledHeight - 3, 3, 3, 4, 4, 3, 3, 7, 7);
    // Bottom
    guiGraphics.blit(CANVAS_PENDING_BORDER_TEXTURE_RESOURCE, 3, scaledHeight - 3, scaledWidth - 6, 3, 3, 4, 1, 3, 7, 7);
    // Bottom left
    guiGraphics.blit(CANVAS_PENDING_BORDER_TEXTURE_RESOURCE, 0, scaledHeight - 3, 3, 3, 0, 4, 3, 3, 7, 7);
    // Left
    guiGraphics.blit(CANVAS_PENDING_BORDER_TEXTURE_RESOURCE, 0, 3, 3, scaledHeight - 6, 0, 3, 3, 1, 7, 7);

    // Center
    guiGraphics.blit(CANVAS_PENDING_BORDER_TEXTURE_RESOURCE, 3, 3, scaledWidth - 6, scaledHeight - 6, 3, 3, 1, 1, 7, 7);

    // Loading
    this.loaderFrame += this.loaderTimer.advanceTime(Util.getMillis()) % 4;
    if (this.loaderFrame >= 4) {
      this.loaderFrame = 0;
    }

    if (this.loaderFrame == 3) {
      guiGraphics.blit(CANVAS_PENDING_LOADER_TEXTURE_RESOURCE, scaledWidth / 2 - 8, scaledHeight / 2 - 5, 0, 10, 16, 10, 16, 30);
    } else {
      guiGraphics.blit(CANVAS_PENDING_LOADER_TEXTURE_RESOURCE, scaledWidth / 2 - 8, scaledHeight / 2 - 5, 0, 10 * this.loaderFrame, 16, 10, 16, 30);
    }

    pose.popPose();
  }

  @Override
  public int getHeight() {
    if (getCanvasCode(this.component.stack) == null) {
      return 0;
    }

    final int[] size = getCanvasSize(this.component.stack);
    final int width = size[0];
    final int height = size[1];

    return (int) (getScale(width, height) * height) + 4;
  }

  @Override
  public int getWidth(Font font) {
    if (getCanvasCode(this.component.stack) == null) {
      return 0;
    }

    final int[] size = getCanvasSize(this.component.stack);
    final int width = size[0];
    final int height = size[1];

    return (int) (getScale(width, height) * width);
  }

  private static String getCanvasCode(ItemStack stack) {
    String canvasCode;

    if (stack.is(ZetterItems.CANVAS.get())) {
      canvasCode = CanvasItem.getCanvasCode(stack);
    } else {
      canvasCode = PaintingItem.getPaintingCode(stack);
    }

    if (canvasCode == null || canvasCode.isEmpty()) {
      return null;
    }

    return canvasCode;
  }

  /**
   * Fetches size in pixels from stack NBT
   * Works even if canvas is not loaded
   * @param itemStack
   * @return
   */
  private static int[] getCanvasSize(ItemStack itemStack) {
    final int[] size = CanvasItem.getBlockSize(itemStack);

    if (size == null) {
      return new int[] { Helper.getResolution().getNumeric(), Helper.getResolution().getNumeric() };
    }

    final int width = size[0] * CanvasItem.getResolution(itemStack);
    final int height = size[1] * CanvasItem.getResolution(itemStack);

    return new int[] { width, height };
  }

  /**
   * Calculates scale for canvas from nearest power of two
   * @param width in pixels
   * @param height in pixels
   * @return
   */
  private static float getScale(int width, int height) {
    return Math.min(1.0F / ((getNearestPowerOfTwo(width) / 64.0F)), 1.0F / ((getNearestPowerOfTwo(height) / 64.0F)));
  }

  private static int getNearestPowerOfTwo(int value) {
    int v = value;

    v--;
    v |= (v >> 1);
    v |= (v >> 2);
    v |= (v >> 4);
    v |= (v >> 8);
    v++;

    return v;
  }

  public static record CanvasComponent(ItemStack stack) implements TooltipComponent {
  }
}
