package me.dantaeusb.zetter.client.gui.painting.tabs;

import com.google.common.collect.Lists;
import me.dantaeusb.zetter.client.gui.PaintingScreen;
import me.dantaeusb.zetter.client.gui.painting.AbstractPaintingWidget;
import net.minecraft.network.chat.Component;

import java.util.List;

public class AbstractTab extends AbstractPaintingWidget {
    private static final int POSITION_X = 37;
    private static final int POSITION_Y = 155;
    private static final int WIDTH = 162;
    private static final int HEIGHT = 76;

    protected final List<AbstractPaintingWidget> tabWidgets = Lists.newArrayList();

    public AbstractTab(PaintingScreen parentScreen, int windowX, int windowY, Component title) {
        super(parentScreen, windowX + POSITION_X, windowY + POSITION_Y, WIDTH, HEIGHT, title);
    }

    public void addTabWidget(AbstractPaintingWidget widget) {
        this.tabWidgets.add(widget);
    }

    public void containerTick() {

    }
}
