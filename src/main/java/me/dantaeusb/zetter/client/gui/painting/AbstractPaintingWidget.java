package me.dantaeusb.zetter.client.gui.painting;

import me.dantaeusb.zetter.client.gui.PaintingScreen;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;

abstract public class AbstractPaintingWidget extends AbstractWidget implements GuiEventListener {
    protected final PaintingScreen parentScreen;

    public AbstractPaintingWidget(PaintingScreen parentScreen, int x, int y, int width, int height, Component title) {
        super(x, y, width, height, title);

        this.parentScreen = parentScreen;
    }

    public @Nullable Component getTooltip(int mouseX, int mouseY) {
        return this.getMessage();
    }

    /**
     * @todo: Would be nice to make actual narrations, it seems quite easy with widget system
     * @param narratorOutput
     */
    @Override
    public void updateNarration(NarrationElementOutput narratorOutput) {
        this.defaultButtonNarrationText(narratorOutput);
    }
}
