package me.dantaeusb.zetter.client.gui.painting.tool;

import me.dantaeusb.zetter.client.gui.PaintingScreen;
import me.dantaeusb.zetter.client.gui.painting.base.TabsWidget;
import me.dantaeusb.zetter.client.gui.painting.tool.brush.BrushParametersWidget;
import me.dantaeusb.zetter.painting.Tools;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.network.chat.Component;

import java.util.Map;

public class BrushTabGroupWidget extends AbstractToolTabGroupWidget implements Renderable {
  public BrushTabGroupWidget(PaintingScreen parentScreen) {
    super(
        parentScreen,
        Component.translatable("screen.zetter.painting.tool.brush"),
        Tools.BRUSH,
        new TabsWidget.Tab[]{
            new TabsWidget.Tab("brush", Component.translatable("screen.zetter.painting.tool.brush"), parentScreen.getFont()),
        }, Map.of(
            "brush", new BrushParametersWidget(
                parentScreen, X, Y + 13, WIDTH, HEIGHT - 13, Component.translatable("screen.zetter.painting.tool.brush")
            )
        ));
  }
}
