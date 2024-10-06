package me.dantaeusb.zetter.client.gui.painting.tool;

import me.dantaeusb.zetter.client.gui.PaintingScreen;
import me.dantaeusb.zetter.client.gui.painting.AbstractPaintingWidget;
import me.dantaeusb.zetter.client.gui.painting.base.TabsWidget;
import me.dantaeusb.zetter.painting.Tools;
import net.minecraft.network.chat.Component;

import java.util.Map;

public abstract class AbstractToolTabGroupWidget extends AbstractTabGroupWidget {
  protected final static int X = 31;
  protected final static int Y = 135;

  protected final static int WIDTH = 164;
  protected final static int HEIGHT = 120;

  private final Tools tool;

  public AbstractToolTabGroupWidget(PaintingScreen parentScreen, Component title, Tools tool, TabsWidget.Tab[] tabs, Map<String, AbstractPaintingWidget> tabWidgets) {
    super(parentScreen, X, Y, WIDTH, HEIGHT, title, tabs, tabWidgets);

    this.tool = tool;
  }

  public Tools getTool() {
    return this.tool;
  }
}
