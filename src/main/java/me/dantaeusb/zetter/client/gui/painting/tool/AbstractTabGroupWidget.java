package me.dantaeusb.zetter.client.gui.painting.tool;

import me.dantaeusb.zetter.client.gui.PaintingScreen;
import me.dantaeusb.zetter.client.gui.painting.AbstractPaintingWidget;
import me.dantaeusb.zetter.client.gui.painting.base.TabsWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.Map;

public abstract class AbstractTabGroupWidget extends AbstractPaintingWidget {
  private final TabsWidget tabsWidget;
  private final Map<String, AbstractPaintingWidget> tabWidgets;

  public AbstractTabGroupWidget(PaintingScreen parentScreen, int x, int y, int width, int height, Component title, TabsWidget.Tab[] tabs, Map<String, AbstractPaintingWidget> tabWidgets) {
    super(parentScreen, x, y, width, height, title);

    final int TABS_WIDGET_POSITION_X = 0;
    final int TABS_WIDGET_POSITION_Y = 0;

    this.tabsWidget = new TabsWidget(parentScreen, x + TABS_WIDGET_POSITION_X, y + TABS_WIDGET_POSITION_Y, Component.translatable("screen.zetter.painting.tool.tabs"), this::onSwitchTab, tabs);
    this.tabWidgets = tabWidgets;

    this.onSwitchTab(this.tabsWidget.getActiveTabCode());
  }

  private void onSwitchTab(String tabName) {
    for (Map.Entry<String, AbstractPaintingWidget> entry : this.tabWidgets.entrySet()) {
      entry.getValue().active = entry.getKey().equals(tabName);
      entry.getValue().visible = entry.getKey().equals(tabName);
    }
  }

  @Override
  public boolean mouseClicked(double mouseX, double mouseY, int button) {
    if (this.tabsWidget.mouseClicked(mouseX, mouseY, button)) {
      return true;
    }

    for (Map.Entry<String, AbstractPaintingWidget> entry : this.tabWidgets.entrySet()) {
      if (entry.getValue().visible && entry.getValue().mouseClicked(mouseX, mouseY, button)) {
        return true;
      }
    }

    return false;
  }

  @Override
  protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    this.tabsWidget.render(guiGraphics, mouseX, mouseY, partialTick);

    for (Map.Entry<String, AbstractPaintingWidget> entry : this.tabWidgets.entrySet()) {
      if (entry.getValue().visible) {
        entry.getValue().render(guiGraphics, mouseX, mouseY, partialTick);
      }
    }
  }
}
