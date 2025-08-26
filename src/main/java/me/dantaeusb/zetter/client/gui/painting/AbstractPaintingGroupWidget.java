package me.dantaeusb.zetter.client.gui.painting;

import com.google.common.collect.Lists;
import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.client.gui.PaintingScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;

abstract public class AbstractPaintingGroupWidget extends AbstractPaintingWidget implements GuiEventListener {
    private final List<AbstractPaintingWidget> widgets = Lists.newArrayList();

    public AbstractPaintingGroupWidget(PaintingScreen parentScreen, int x, int y, int width, int height, Component title) {
        super(parentScreen, x, y, width, height, title);
    }

    public void addWidget(AbstractPaintingWidget widget) {
        this.widgets.add(widget);
    }

    public List<AbstractPaintingWidget> getWidgets() {
        return this.widgets;
    }

    @Override
    public void renderTooltip(@NotNull GuiGraphics guiGraphics, int x, int y) {
        for (AbstractPaintingWidget widget : this.widgets) {
            widget.renderTooltip(guiGraphics, x, y);
        }
    }

    @Override
    public @Nullable Component getTooltip(int mouseX, int mouseY) {
        for (AbstractPaintingWidget widget : this.widgets) {
            if (widget.visible) {
                Component tooltip = widget.getTooltip(mouseX, mouseY);
                if (tooltip != null) {
                    return tooltip;
                }
            }
        }
        return null;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.isMouseOver(mouseX, mouseY) || !this.isValidClickButton(button)) {
            return false;
        }

        for (AbstractPaintingWidget widget : this.widgets) {
            if (widget.visible && widget.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (!this.isMouseOver(mouseX, mouseY) || !this.isValidClickButton(button)) {
            return false;
        }

        for (AbstractPaintingWidget widget : this.widgets) {
            if (widget.visible && widget.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (!this.isMouseOver(mouseX, mouseY) || !this.isValidClickButton(button)) {
            return false;
        }

        for (AbstractPaintingWidget widget : this.widgets) {
            if (widget.visible && widget.mouseReleased(mouseX, mouseY, button)) {
                return true;
            }
        }

        return false;
    }
}
