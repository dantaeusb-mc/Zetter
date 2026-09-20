package me.dantaeusb.zetter.client.gui.painting.tool;

import me.dantaeusb.zetter.client.gui.PaintingScreen;
import me.dantaeusb.zetter.client.gui.painting.AbstractPaintingGroupWidget;
import me.dantaeusb.zetter.client.gui.painting.base.OptionsWidget;
import me.dantaeusb.zetter.client.gui.painting.base.SliderWidget;
import me.dantaeusb.zetter.painting.parameters.BlendingParameterHolder;
import me.dantaeusb.zetter.painting.parameters.DitheringParameterHolder;
import me.dantaeusb.zetter.painting.pipes.BlendingPipe;
import me.dantaeusb.zetter.painting.pipes.DitheringPipe;
import me.dantaeusb.zetter.client.gui.painting.util.ZetterColorPickerRenderer;
import me.dantaeusb.zetter.core.ZetterRenderTypes;
import me.dantaeusb.zetter.core.tools.Color;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.function.Supplier;

/**
 * Tools describe themselves with the same sliders laid out the same way,
 * only the parameters they read and write are different
 */
public abstract class AbstractToolParametersWidget extends AbstractPaintingGroupWidget {
  protected final static int SLIDER_POSITION_X = 5;
  protected final static int SLIDER_DISTANCE_GAP = 14;

  /**
   * Option buttons live next to each other in one strip of the widgets texture:
   * four dithering patterns first, then the two blending modes
   */
  private final static int DITHERING_BUTTONS_U = 108;
  private final static int BLENDING_BUTTONS_U = 188;
  private final static int OPTION_BUTTONS_V = 163;

  /**
   * Both rows of options share a line, with their labels above them
   */
  protected final static int OPTIONS_LABEL_GAP = 10;
  protected final static int DITHERING_POSITION_X = SLIDER_POSITION_X;
  protected final static int BLENDING_POSITION_X = 95;

  /**
   * Checkerboard of the widgets texture, as tall as the groove of a dragged slider
   */
  private final static int CHECKERBOARD_U = 102;
  private final static int CHECKERBOARD_V = 139;

  /**
   * Notched track of the size slider, as tall as the groove of a dragged slider:
   * a stretch of it with no notch on it, and the notch on its own to stamp over
   */
  private final static int TRACK_V = 147;
  private final static int TRACK_PLAIN_U = 103;
  private final static int TRACK_PLAIN_WIDTH = 16;
  private final static int TRACK_NOTCH_U = 119;

  public AbstractToolParametersWidget(PaintingScreen parentScreen, int x, int y, int width, int height, Component title) {
    super(parentScreen, x, y, width, height, title);
  }

  protected OptionsWidget<DitheringPipe.DitheringOption> createDitheringWidget(int x, int y, Supplier<DitheringParameterHolder> parameters) {
    return new OptionsWidget<>(
        this.parentScreen, x, y,
        Component.translatable("container.zetter.painting.dithering"),
        DitheringPipe.DitheringOption.values(),
        option -> option.translatableComponent,
        () -> parameters.get().getDithering(),
        option -> parameters.get().setDithering(option),
        DITHERING_BUTTONS_U, OPTION_BUTTONS_V
    );
  }

  protected OptionsWidget<BlendingPipe.BlendingOption> createBlendingWidget(int x, int y, Supplier<BlendingParameterHolder> parameters) {
    return new OptionsWidget<>(
        this.parentScreen, x, y,
        Component.translatable("container.zetter.painting.blending"),
        BlendingPipe.BlendingOption.values(),
        option -> option.translatableComponent,
        () -> parameters.get().getBlending(),
        option -> parameters.get().setBlending(option),
        BLENDING_BUTTONS_U, OPTION_BUTTONS_V
    );
  }

  /**
   * Amount of whole pixel sizes the tool can be set to, a notch is painted for each.
   * Asked for every time the track is drawn, as bigger canvas resolutions are going
   * to allow for bigger sizes.
   */
  protected int getSizeNotches() {
    return 0;
  }

  /**
   * Rows of option buttons are labelled the same way the sliders are, with the
   * label sitting right above the buttons it names
   */
  protected final void renderOptions(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, int positionY, OptionsWidget<?>... widgets) {
    for (OptionsWidget<?> widget : widgets) {
      guiGraphics.drawString(
          this.parentScreen.getFont(),
          widget.getMessage().getString(),
          widget.getX(),
          this.getY() + positionY - OPTIONS_LABEL_GAP,
          Color.DARK_GRAY.getARGB(), false
      );

      widget.render(guiGraphics, mouseX, mouseY, partialTick);
    }
  }

  /**
   * Current color over the checkerboard, fully transparent on the left and fully
   * opaque on the right, so the checkerboard shows through exactly as much as the
   * paint will let the canvas show through
   */
  protected void renderIntensityBackground(GuiGraphics guiGraphics, int x, int y, int width, int height, float value) {
    guiGraphics.blit(PAINTING_WIDGETS_TEXTURE_RESOURCE, x, y, CHECKERBOARD_U, CHECKERBOARD_V, width, height);

    final boolean okHsl = this.parentScreen.getEaselState().colorSpace() == PaintingScreen.ColorSpace.okHSL;

    ZetterColorPickerRenderer.renderColorPicker(
        guiGraphics,
        okHsl ? ZetterRenderTypes.RenderMode.OK_OPACITY_HORIZONTAL : ZetterRenderTypes.RenderMode.RGB_OPACITY_HORIZONTAL,
        okHsl ? this.parentScreen.getCurrentColor().getOkHsl() : this.parentScreen.getCurrentColor().getHsl(),
        x,
        y,
        width,
        height
    );
  }

  /**
   * Track with a notch for every size the tool can be set to, repeated out of the
   * widgets texture and stamped with a notch per value.
   *
   * Painted rather than filled with a color picked here: players reskin these
   * screens, and a color written into the code is one they cannot reach.
   */
  protected void renderSizeBackground(GuiGraphics guiGraphics, int x, int y, int width, int height, float value) {
    final int notches = this.getSizeNotches();

    if (notches <= 0) {
      return;
    }

    // Anchored to the bottom edge, where the notches stand, so the track keeps its
    // footing while the groove opens up around the handle being dragged
    final int v = TRACK_V + SliderWidget.HORIZONTAL_CONTENT_DRAGGING_HEIGHT - height;

    guiGraphics.blitRepeating(
        PAINTING_WIDGETS_TEXTURE_RESOURCE,
        x, y, width, height,
        TRACK_PLAIN_U, v, TRACK_PLAIN_WIDTH, height
    );

    for (int i = 0; i < notches; i++) {
      // Single value slider would only have a notch where it starts
      final float notchValue = notches > 1 ? (float) i / (notches - 1) : 0.0f;
      final int offset = SliderWidget.getHorizontalHandlerOffset(notchValue);

      if (offset < 0 || offset >= width) {
        continue;
      }

      guiGraphics.blit(PAINTING_WIDGETS_TEXTURE_RESOURCE, x + offset, y, TRACK_NOTCH_U, v, 1, height);
    }
  }

  /**
   * Nothing is painted over the handle for now
   */
  protected void renderHandlerState(GuiGraphics guiGraphics, int x, int y, int width, int height, float value) {
  }
}
