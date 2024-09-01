package me.dantaeusb.zetter.client.gui.tooltip;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import me.dantaeusb.zetter.capability.canvastracker.CanvasTracker;
import me.dantaeusb.zetter.client.renderer.CanvasRenderer;
import me.dantaeusb.zetter.core.Helper;
import me.dantaeusb.zetter.core.ZetterItems;
import me.dantaeusb.zetter.item.CanvasItem;
import me.dantaeusb.zetter.item.PaintingItem;
import me.dantaeusb.zetter.storage.AbstractCanvasData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

public class CanvasTooltipRenderer implements ClientTooltipComponent {
  private final CanvasComponent component;

  public CanvasTooltipRenderer(CanvasComponent component) {
    this.component = component;
  }

  @Override
  public void renderImage(Font font, int tooltipX, int tooltipY, GuiGraphics guiGraphics) {
    Minecraft mc = Minecraft.getInstance();
    PoseStack pose = guiGraphics.pose();

    ItemStack stack = this.component.stack();
    String canvasCode = getCanvasCode(stack);

    if (mc.level == null || canvasCode == null) {
      return;
    }

    CanvasTracker canvasTracker = Helper.getLevelCanvasTracker(mc.level);
    AbstractCanvasData canvasData = canvasTracker.getCanvasData(canvasCode);

    if (canvasData == null) {
      CanvasRenderer.getInstance().queueCanvasTextureUpdate(canvasCode);
      return;
    }

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
  }

  @Override
  public int getHeight() {
    Minecraft mc = Minecraft.getInstance();
    String canvasCode = getCanvasCode(this.component.stack());

    if (mc.level == null || canvasCode == null) {
      return 0;
    }

    CanvasTracker canvasTracker = Helper.getLevelCanvasTracker(mc.level);
    AbstractCanvasData canvasData = canvasTracker.getCanvasData(canvasCode);

    if (canvasData == null) {
      return 68;
    }

    return (int) (getScale(canvasData.getWidth(), canvasData.getHeight()) * canvasData.getHeight()) + 4;
  }

  @Override
  public int getWidth(Font font) {
    Minecraft mc = Minecraft.getInstance();
    String canvasCode = getCanvasCode(this.component.stack());

    if (mc.level == null || canvasCode == null) {
      return 0;
    }

    CanvasTracker canvasTracker = Helper.getLevelCanvasTracker(mc.level);
    AbstractCanvasData canvasData = canvasTracker.getCanvasData(canvasCode);

    if (canvasData == null) {
      return 64;
    }

    return (int) (getScale(canvasData.getWidth(), canvasData.getHeight()) * canvasData.getWidth());
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
