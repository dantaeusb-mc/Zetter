package me.dantaeusb.zetter.client.gui.painting;

import com.mojang.blaze3d.systems.RenderSystem;
import me.dantaeusb.zetter.client.gui.PaintingScreen;
import me.dantaeusb.zetter.painting.Tools;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class CanvasModeWidget extends AbstractPaintingWidget implements Renderable {
  private final List<ModeButton> buttons;

  final static int MODE_BUTTON_WIDTH = 22;
  final static int MODE_BUTTON_HEIGHT = 16;

  final static int MODE_BUTTONS_U = 143;
  final static int MODE_BUTTONS_V = 0;

  public CanvasModeWidget(PaintingScreen parentScreen, int x, int y) {
    // Add borders
    super(parentScreen, x, y, MODE_BUTTON_WIDTH + 2, MODE_BUTTON_HEIGHT * 5 + 2, Component.translatable("container.zetter.painting.tools"));

    this.buttons = new ArrayList<>() {{
      add(new ModeButton(PaintingScreen.CanvasMode.IMMERSIVE, MODE_BUTTONS_U + MODE_BUTTON_WIDTH + 3, MODE_BUTTONS_V + 1, MODE_BUTTON_WIDTH, MODE_BUTTON_HEIGHT));
      add(new ModeButton(PaintingScreen.CanvasMode.IMMERSIVE_BACKGROUND, MODE_BUTTONS_U + MODE_BUTTON_WIDTH + 3, MODE_BUTTONS_V + MODE_BUTTON_HEIGHT + 1, MODE_BUTTON_WIDTH, MODE_BUTTON_HEIGHT));
      add(new ModeButton(PaintingScreen.CanvasMode.OVERLAY, MODE_BUTTONS_U + MODE_BUTTON_WIDTH + 3, MODE_BUTTONS_V + MODE_BUTTON_HEIGHT * 2 + 1, MODE_BUTTON_WIDTH, MODE_BUTTON_HEIGHT));
    }};
  }

  @Override
  public @Nullable
  Component getTooltip(int mouseX, int mouseY) {
    int i = 0;
    for (ModeButton modeButton : this.buttons) {
      int fromY = this.getY() + 1 + i * MODE_BUTTON_HEIGHT;

      if (isInRect(this.getX() + 1, fromY, MODE_BUTTON_WIDTH, MODE_BUTTON_HEIGHT, mouseX, mouseY)) {
        return modeButton.getTooltip();
      }

      i++;
    }

    return null;
  }

  @Override
  public boolean mouseClicked(double mouseX, double mouseY, int button) {
    int iMouseX = (int) mouseX;
    int iMouseY = (int) mouseY;

    // Quick check
    if (!this.isMouseOver(mouseX, mouseY)) {
      return false;
    }

    int i = 0;
    for (ModeButton modeButton : this.buttons) {
      int fromY = this.getY() + 1 + i * MODE_BUTTON_HEIGHT;

      if (isInRect(this.getX(), fromY, modeButton.width, modeButton.height, iMouseX, iMouseY) && this.isValidClickButton(button)) {
        this.parentScreen.setPaintingScreenState(this.parentScreen.getPaintingScreenState().withCanvasMode(modeButton.mode));
        this.playDownSound(Minecraft.getInstance().getSoundManager());
        return true;
      }

      i++;
    }

    return false;
  }

  @Override
  protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
    narrationElementOutput.add(NarratedElementType.TITLE, this.createNarrationMessage());
  }

  @Override
  public void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
    RenderSystem.setShader(GameRenderer::getPositionTexShader);
    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    RenderSystem.setShaderTexture(0, PAINTING_WIDGETS_TEXTURE_RESOURCE);

    guiGraphics.blit(PAINTING_WIDGETS_TEXTURE_RESOURCE, this.getX(), this.getY(), MODE_BUTTONS_U, MODE_BUTTONS_V, MODE_BUTTON_WIDTH + 2, MODE_BUTTON_HEIGHT * this.buttons.size() + 2);

    int i = 0;
    for (ModeButton modeButton : this.buttons) {
      int fromY = this.getY() + 1 + i * MODE_BUTTON_HEIGHT;
      int uOffset = modeButton.uPosition + (this.parentScreen.getPaintingScreenState().canvasMode() == modeButton.mode ? MODE_BUTTON_WIDTH + 2 : 0);

      guiGraphics.blit(PAINTING_WIDGETS_TEXTURE_RESOURCE, this.getX() + 1, fromY, uOffset, modeButton.vPosition, modeButton.width, modeButton.height);
      i++;
    }
  }

  public static class ModeButton {
    public final PaintingScreen.CanvasMode mode;
    public final int uPosition;
    public final int vPosition;
    public final int height;
    public final int width;

    ModeButton(PaintingScreen.CanvasMode mode, int uPosition, int vPosition, int width, int height) {
      this.mode = mode;
      this.uPosition = uPosition;
      this.vPosition = vPosition;
      this.height = height;
      this.width = width;
    }

    public Component getTooltip() {
      return switch (this.mode) {
        case IMMERSIVE -> Component.translatable("screen.zetter.painting.canvas_mode.immersive");
        case IMMERSIVE_BACKGROUND -> Component.translatable("screen.zetter.painting.canvas_mode.immersive_background");
        case OVERLAY -> Component.translatable("screen.zetter.painting.canvas_mode.overlay");
      };
    }
  }
}
