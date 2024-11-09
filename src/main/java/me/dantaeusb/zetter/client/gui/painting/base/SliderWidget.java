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
import java.util.function.Supplier;

public class SliderWidget extends AbstractPaintingWidget implements Renderable {
  /**
   * Size in horizontal mode, swapped in vertical mode
   */
  public final static int HORIZONTAL_WIDTH = 150;
  public final static int HORIZONTAL_HEIGHT = 9;

  public final static int VERTICAL_WIDTH = 9;
  public final static int VERTICAL_HEIGHT = 120;

  private final Orientation orientation;

  private boolean sliderDragging = false;

  /**
   * The function to paint slider's background (i.e. checkerboard)
   */
  private final @Nullable PaintConsumer backgroundLambda;
  /**
   * The function to paint slider's foreground (i.e. gradient)
   */
  private final @Nullable PaintConsumer handlerLambda;
  private final @NotNull Supplier<Float> valueSupplier;
  private Consumer<Float> positionConsumer;

  public SliderWidget(
      PaintingScreen parentScreen, int x, int y, Component translatableComponent,
      Supplier<Float> valueSupplier,
      Consumer<Float> positionConsumer,
      Orientation orientation,
      @Nullable PaintConsumer backgroundLambda, @Nullable PaintConsumer handlerLambda
  ) {
    super(
        parentScreen, x, y,
        orientation == Orientation.HORIZONTAL ? HORIZONTAL_WIDTH : VERTICAL_WIDTH,
        orientation == Orientation.HORIZONTAL ? HORIZONTAL_HEIGHT : VERTICAL_HEIGHT,
        translatableComponent
    );

    this.orientation = orientation;

    this.valueSupplier = valueSupplier;
    this.positionConsumer = positionConsumer;

    this.backgroundLambda = backgroundLambda;
    this.handlerLambda = handlerLambda;
  }

  public SliderWidget(
      PaintingScreen parentScreen, int x, int y, Component translatableComponent,
      Supplier<Float> valueSupplier,
      Consumer<Float> positionConsumer,
      @Nullable PaintConsumer backgroundLambda, @Nullable PaintConsumer handlerLambda
  ) {
    this(parentScreen, x, y, translatableComponent, valueSupplier, positionConsumer, Orientation.HORIZONTAL, backgroundLambda, handlerLambda);
  }

  @Override
  public @Nullable Component getTooltip(int mouseX, int mouseY) {
    return null;
  }

  @Override
  public boolean mouseClicked(double mouseX, double mouseY, int button) {
    if (!this.isMouseOver(mouseX, mouseY) || !this.isValidClickButton(button)) {
      return false;
    }

    int iMouseX = (int) mouseX;
    int iMouseY = (int) mouseY;

    this.handleSliderInteraction(iMouseX, iMouseY);
    return true;
  }

  @Override
  protected void onDrag(double mouseX, double mouseY, double dragX, double dragY) {
    if (this.sliderDragging) {
      this.handleSliderInteraction(mouseX, mouseY);
    }
  }

  @Override
  public void onRelease(double mouseX, double mouseY) {
    this.sliderDragging = false;
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

    float value = this.valueSupplier.get();

    if (this.orientation == Orientation.HORIZONTAL) {
      int sliderV = SLIDER_HORIZONTAL_POSITION_V;

      if (this.sliderDragging) {
        sliderV += HORIZONTAL_HEIGHT;
      }

      guiGraphics.blit(PAINTING_WIDGETS_TEXTURE_RESOURCE, this.getX(), this.getY(), SLIDER_HORIZONTAL_POSITION_U, sliderV, HORIZONTAL_WIDTH, HORIZONTAL_HEIGHT);

      if (this.backgroundLambda != null) {
        int sliderContentWidth = HORIZONTAL_WIDTH - 6;
        int sliderContentHeight = 3;

        if (this.sliderDragging) {
          sliderContentGlobalTop -= 2;
          sliderContentHeight += 4;
        }

        this.backgroundLambda.accept(guiGraphics, sliderContentGlobalLeft, sliderContentGlobalTop, sliderContentWidth, sliderContentHeight, value);
      }
    } else {
      int sliderU = SLIDER_VERTICAL_POSITION_U;

      if (this.sliderDragging) {
        sliderU += VERTICAL_WIDTH;
      }

      guiGraphics.blit(PAINTING_WIDGETS_TEXTURE_RESOURCE, this.getX(), this.getY(), sliderU, SLIDER_VERTICAL_POSITION_V, VERTICAL_WIDTH, VERTICAL_HEIGHT);

      if (this.backgroundLambda != null) {
        int sliderContentWidth = 3;
        int sliderContentHeight = VERTICAL_HEIGHT - 6;

        if (this.sliderDragging) {
          sliderContentGlobalLeft -= 2;
          sliderContentWidth += 4;
        }

        this.backgroundLambda.accept(guiGraphics, sliderContentGlobalLeft, sliderContentGlobalTop, sliderContentWidth, sliderContentHeight, value);
      }
    }
  }

  /**
   * @param mouseX
   * @param mouseY
   */
  protected void handleSliderInteraction(final double mouseX, final double mouseY) {
    this.sliderDragging = true;

    float percent;

    if (this.orientation == Orientation.HORIZONTAL) {
      percent = (float) (mouseX - this.getX() - 3) / (HORIZONTAL_WIDTH - 7);
    } else {
      percent = 1.0f - (float) (mouseY - this.getY() - 3) / (VERTICAL_HEIGHT - 7);
    }

    percent = Mth.clamp(percent, 0.0f, 1.0f);

    this.positionConsumer.accept(percent);
  }

  /**
   * Handlers
   */

  protected void drawHandler(GuiGraphics guiGraphics) {
    final int HANDLER_HORIZONTAL_POSITION_U = 90;
    final int HANDLER_HORIZONTAL_POSITION_V = 119;
    final int HANDLER_VERTICAL_POSITION_U = 90;
    final int HANDLER_VERTICAL_POSITION_V = 141;

    final int HANDLER_WIDTH = 5;
    final int HANDLER_HEIGHT = 11;

    float value = this.valueSupplier.get();

    if (this.orientation == Orientation.HORIZONTAL) {
      int sliderContentWidth = HORIZONTAL_WIDTH - 7;

      int sliderGlobalLeft = this.getX() + (int) (sliderContentWidth * value) + 3 - 2;
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

        this.handlerLambda.accept(guiGraphics, offsetX, offsetY, width, height, value);
      }
    } else {
      int sliderContentHeight = VERTICAL_HEIGHT - 7;

      int sliderGlobalLeft = this.getX() - 1;
      int sliderGlobalTop = this.getY() + (int) (sliderContentHeight * (1.0f - value)) + 3 - 2;

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

        this.handlerLambda.accept(guiGraphics, offsetX, offsetY, width, height, value);
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
