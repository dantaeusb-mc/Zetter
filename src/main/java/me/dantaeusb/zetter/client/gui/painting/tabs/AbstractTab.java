package me.dantaeusb.zetter.client.gui.painting.tabs;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;
import me.dantaeusb.zetter.client.gui.PaintingScreen;
import me.dantaeusb.zetter.client.gui.painting.AbstractPaintingWidget;
import net.minecraft.network.chat.Component;

import java.util.List;

public class AbstractTab extends AbstractPaintingWidget {
    public static final int POSITION_X = 37;
    public static final int POSITION_Y = 155;
    public static final int WIDTH = 162;
    public static final int HEIGHT = 76;

    protected final List<AbstractPaintingWidget> tabWidgets = Lists.newArrayList();

    public AbstractTab(PaintingScreen parentScreen, int windowX, int windowY, Component title) {
        super(parentScreen, windowX + POSITION_X, windowY + POSITION_Y, WIDTH, HEIGHT, title);
    }

    public void addTabWidget(AbstractPaintingWidget widget) {
        this.tabWidgets.add(widget);
    }

    public void renderLabels(PoseStack matrixStack, int mouseX, int mouseY) {
        for (AbstractPaintingWidget widget : this.tabWidgets) {
            widget.renderLabels(matrixStack, mouseX, mouseY);
        }
    }

    public void containerTick() {

    }
}
