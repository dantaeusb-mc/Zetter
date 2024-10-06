package me.dantaeusb.zetter.client.gui.painting.base;

import me.dantaeusb.zetter.client.gui.PaintingScreen;
import me.dantaeusb.zetter.client.gui.painting.AbstractPaintingWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.function.Consumer;

public class SliderWidget extends AbstractPaintingWidget implements Renderable {
  /**
   * Size in horizontal mode, swapped in vertical mode
   */
  final static int WIDTH = 150;
  final static int HEIGHT = 9;

  private final Orientation orientation;

  private float value = 0.0f;
  private boolean sliderDragging = false;

  /**
   * The function to paint slider's background (i.e. checkerboard)
   */
  private final @Nullable PaintConsumer backgroundLambda;
  /**
   * The function to paint slider's foreground (i.e. gradient)
   */
  private final @Nullable PaintConsumer handlerLambda;
  private Consumer<Float> positionConsumer;

  public SliderWidget(
      PaintingScreen parentScreen, int x, int y, Component translatableComponent,
      Consumer<Float> positionConsumer,
      Orientation orientation,
      @Nullable PaintConsumer backgroundLambda, @Nullable PaintConsumer handlerLambda
  ) {
    super(
        parentScreen, x, y,
        orientation == Orientation.HORIZONTAL ? WIDTH : HEIGHT,
        orientation == Orientation.HORIZONTAL ? HEIGHT : WIDTH,
        translatableComponent
    );

    this.orientation = orientation;

    this.backgroundLambda = backgroundLambda;
    this.handlerLambda = handlerLambda;

    this.positionConsumer = positionConsumer;
  }

  public SliderWidget(
      PaintingScreen parentScreen, int x, int y, Component translatableComponent,
      Consumer<Float> positionConsumer,
      @Nullable PaintConsumer backgroundLambda, @Nullable PaintConsumer handlerLambda
  ) {
    this(parentScreen, x, y, translatableComponent, positionConsumer, Orientation.HORIZONTAL, backgroundLambda, handlerLambda);
  }

  public double getValue() {
    return this.value;
  }

  public void setValue(float percent) {
    this.value = percent;
  }

  @Override
  public @Nullable Component getTooltip(int mouseX, int mouseY) {
    return null;
  }

  @Override
  public boolean mouseClicked(double mouseX, double mouseY, int button) {
    int iMouseX = (int) mouseX;
    int iMouseY = (int) mouseY;

    if (this.isMouseOver(mouseX, mouseY)) {
      this.handleSliderInteraction(iMouseX, iMouseY);
      return true;
    }

    return super.mouseClicked(mouseX, mouseY, button);
  }

  @Override
  public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
    if (this.sliderDragging) {
      this.handleSliderInteraction(mouseX, mouseY);
      return true;
    }

    return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
  }

  @Override
  public boolean mouseReleased(double mouseX, double mouseY, int button) {
    if (this.sliderDragging) {
      // If we were changing palette colors, sync them with server
      // this.parentScreen.getMenu().sendPaletteUpdatePacket();
    }

    this.sliderDragging = false;

    return super.mouseReleased(mouseX, mouseY, button);
  }

  protected void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
    this.drawSliderBackground(guiGraphics);
    this.drawHandler(guiGraphics);
  }

  protected void drawSliderBackground(GuiGraphics guiGraphics) {
    final int SLIDER_HORIZONTAL_POSITION_U = 106;
    final int SLIDER_HORIZONTAL_POSITION_V = 120;
    final int SLIDER_VERTICAL_POSITION_U = 72;
    final int SLIDER_VERTICAL_POSITION_V = 120;

    int sliderContentGlobalLeft = this.getX() + 3;
    int sliderContentGlobalTop = this.getY() + 3;

    if (this.orientation == Orientation.HORIZONTAL) {
      int sliderV = SLIDER_HORIZONTAL_POSITION_V;

      if (this.sliderDragging) {
        sliderV += HEIGHT;
      }

      /**
       * Sugar, hope the compiler will remove it.
       */
      final int width = WIDTH;
      final int height = HEIGHT;

      guiGraphics.blit(PAINTING_WIDGETS_TEXTURE_RESOURCE, this.getX(), this.getY(), SLIDER_HORIZONTAL_POSITION_U, sliderV, width, height);

      if (this.backgroundLambda != null) {
        int sliderContentWidth = width - 6;
        int sliderContentHeight = 3;

        if (this.sliderDragging) {
          sliderContentGlobalTop -= 2;
          sliderContentHeight += 4;
        }

        this.backgroundLambda.accept(guiGraphics, sliderContentGlobalLeft, sliderContentGlobalTop, sliderContentWidth, sliderContentHeight, this.value);
      }
    } else {
      int sliderU = SLIDER_VERTICAL_POSITION_U;

      if (this.sliderDragging) {
        sliderU += HEIGHT;
      }

      /**
       * Sugar, hope the compiler will remove it.
       */
      final int width = HEIGHT;
      final int height = WIDTH;

      guiGraphics.blit(PAINTING_WIDGETS_TEXTURE_RESOURCE, this.getX(), this.getY(), sliderU, SLIDER_VERTICAL_POSITION_V, width, height);

      if (this.backgroundLambda != null) {
        int sliderContentWidth = 3;
        int sliderContentHeight = height - 6;

        if (this.sliderDragging) {
          sliderContentGlobalLeft -= 2;
          sliderContentWidth += 4;
        }

        this.backgroundLambda.accept(guiGraphics, sliderContentGlobalLeft, sliderContentGlobalTop, sliderContentWidth, sliderContentHeight, this.value);
      }
    }
  }

  /**
   * @param mouseX
   * @param mouseY
   */
  protected void handleSliderInteraction(final double mouseX, final double mouseY) {

    this.sliderDragging = true;

    float percent = (float) (mouseX - this.getX() - 3) / (WIDTH - 7);
    percent = Mth.clamp(percent, 0.0f, 1.0f);

    this.value = percent;

    this.positionConsumer.accept(percent);
  }

  /**
   * Handlers
   */

  protected void drawHandler(GuiGraphics guiGraphics) {
    final int HANDLER_HORIZONTAL_POSITION_U = 90;
    final int HANDLER_HORIZONTAL_POSITION_V = 120;
    final int HANDLER_VERTICAL_POSITION_U = 90;
    final int HANDLER_VERTICAL_POSITION_V = 141;

    final int HANDLER_WIDTH = 5;
    final int HANDLER_HEIGHT = 11;

    if (this.orientation == Orientation.HORIZONTAL) {
      int sliderContentWidth = WIDTH - 7;

      int sliderGlobalLeft = this.getX() + (int) (sliderContentWidth * this.value) + 3 - 2;
      int sliderGlobalTop = this.getY() - 1;

      int sliderV = HANDLER_HORIZONTAL_POSITION_V;

      if (this.sliderDragging) {
        sliderV += HANDLER_HEIGHT;
      }

      guiGraphics.blit(PAINTING_WIDGETS_TEXTURE_RESOURCE, sliderGlobalLeft, sliderGlobalTop, HANDLER_HORIZONTAL_POSITION_U, sliderV, HANDLER_WIDTH, HANDLER_HEIGHT);

      if (this.handlerLambda != null) {
        int offsetX = sliderGlobalLeft;
        int offsetY = sliderGlobalTop;
        int width = 1;
        int height = 3;

        if (this.sliderDragging) {
          offsetX += 1;
          offsetY += 4;
        } else {
          offsetX += 2;
          offsetY += 4;
          width = 3;
        }

        this.handlerLambda.accept(guiGraphics, offsetX, offsetY, width, height, this.value);
      }
    } else {
      int sliderContentHeight = HEIGHT - 7;

      int sliderGlobalLeft = this.getX() - 1;
      int sliderGlobalTop = this.getX() + (int) (sliderContentHeight * this.value) + 3 - 2;

      int sliderV = HANDLER_VERTICAL_POSITION_V;

      if (this.sliderDragging) {
        sliderV += HANDLER_WIDTH;
      }

      // Intentionally swapped width and height
      final int handlerWidth = HANDLER_HEIGHT;
      final int handlerHeight = HANDLER_WIDTH;

      guiGraphics.blit(PAINTING_WIDGETS_TEXTURE_RESOURCE, sliderGlobalLeft, sliderGlobalTop, HANDLER_VERTICAL_POSITION_U, sliderV, handlerWidth, handlerHeight);

      if (this.handlerLambda != null) {
        int offsetX = sliderGlobalLeft;
        int offsetY = sliderGlobalTop;
        int height = 1;
        int width = 3;

        if (this.sliderDragging) {
          offsetX += 4;
          offsetY += 1;
        } else {
          offsetX += 4;
          offsetY += 2;
          height = 3;
        }

        this.handlerLambda.accept(guiGraphics, offsetX, offsetY, width, height, this.value);
      }
    }
  }

  @FunctionalInterface
  public interface PaintConsumer {
    public void accept(GuiGraphics guiGraphics, int x, int y, int width, int height, float value);
  }

  public enum Orientation {
    HORIZONTAL,
    VERTICAL
  }
}
