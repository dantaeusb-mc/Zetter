package com.dantaeusb.zetter.client.gui.painting;

import com.dantaeusb.zetter.client.gui.PaintingScreen;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.util.text.ITextComponent;

import javax.annotation.Nullable;

abstract public class AbstractPaintingWidget extends Widget implements IGuiEventListener {
    protected final PaintingScreen parentScreen;

    public AbstractPaintingWidget(PaintingScreen parentScreen, int x, int y, int width, int height, ITextComponent title) {
        super(x, y, width, height, title);

        this.parentScreen = parentScreen;
    }

    public @Nullable ITextComponent getTooltip(int mouseX, int mouseY) {
        return this.getMessage();
    }
}
