package me.dantaeusb.zetter.client.gui.easel.tabs;

import com.google.common.collect.Lists;
import me.dantaeusb.zetter.client.gui.EaselScreen;
import me.dantaeusb.zetter.client.gui.easel.AbstractEaselWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;

public abstract class AbstractTab extends AbstractEaselWidget {
    public static final int POSITION_X = 37;
    public static final int POSITION_Y = 155;
    public static final int WIDTH = 162;
    public static final int HEIGHT = 76;

    protected final List<AbstractEaselWidget> tabWidgets = Lists.newArrayList();

    public AbstractTab(EaselScreen parentScreen, int windowX, int windowY, Component title) {
        super(parentScreen, windowX + POSITION_X, windowY + POSITION_Y, WIDTH, HEIGHT, title);
    }

    public void addTabWidget(AbstractEaselWidget widget) {
        this.tabWidgets.add(widget);
    }

    public void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        for (AbstractEaselWidget widget : this.tabWidgets) {
            widget.renderLabels(guiGraphics, mouseX, mouseY);
        }
    }

    public void containerTick() {

    }
}
