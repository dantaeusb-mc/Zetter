package me.dantaeusb.zetter.client.gui.painting;

import me.dantaeusb.zetter.client.gui.PaintingScreen;
import me.dantaeusb.zetter.client.gui.painting.base.TabsWidget;
import me.dantaeusb.zetter.client.gui.painting.colorpicker.ColorSlidersWidget;
import me.dantaeusb.zetter.client.gui.painting.colorpicker.ColorWheelWidget;
import me.dantaeusb.zetter.client.gui.painting.tool.AbstractTabGroupWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.network.chat.Component;

import java.util.HashMap;

public class ColorPickerWidget extends AbstractTabGroupWidget implements Renderable {
  private final static int WIDTH = 164;
  private final static int HEIGHT = 135;

  public ColorPickerWidget(PaintingScreen parentScreen, int x, int y) {
    super(
        parentScreen, x, y, WIDTH, HEIGHT,
        Component.translatable("screen.zetter.painting.color_picker"),
        new TabsWidget.Tab[]{
            new TabsWidget.Tab(Mode.WHEEL.code, Component.translatable("screen.zetter.painting.color_picker.wheel"), parentScreen.getFont()),
            new TabsWidget.Tab(Mode.SLIDERS.code, Component.translatable("screen.zetter.painting.color_picker.sliders"), parentScreen.getFont())
        },
        new HashMap<String, AbstractPaintingWidget>() {
          {
            put(Mode.WHEEL.code, new ColorWheelWidget(parentScreen, x, y + 13));
            put(Mode.SLIDERS.code, new ColorSlidersWidget(parentScreen, x, y + 13));
          }
        }
    );
  }

  public enum Mode {
    WHEEL("wheel"),
    SLIDERS("sliders");

    final String code;

    Mode(String code) {
      this.code = code;
    }
  }
}
