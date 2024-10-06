package me.dantaeusb.zetter.client.gui.painting.tool;

import me.dantaeusb.zetter.client.gui.PaintingScreen;
import me.dantaeusb.zetter.client.gui.painting.base.TabsWidget;
import me.dantaeusb.zetter.client.gui.painting.tool.bucket.BucketParametersWidget;
import me.dantaeusb.zetter.painting.Tools;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.network.chat.Component;

import java.util.Map;

public class BucketTabGroupWidget extends AbstractToolTabGroupWidget implements Renderable {
  public BucketTabGroupWidget(PaintingScreen parentScreen) {
    super(
        parentScreen,
        Component.translatable("screen.zetter.painting.tool.bucket"),
        Tools.BUCKET,
        new TabsWidget.Tab[]{
            new TabsWidget.Tab("bucket", Component.translatable("screen.zetter.painting.tool.bucket"), parentScreen.getFont()),
        },
        Map.of(
            "brush", new BucketParametersWidget(
                parentScreen, X, Y + 13, WIDTH, HEIGHT - 13, Component.translatable("screen.zetter.painting.tool.brush")
            )
        ));
  }
}
