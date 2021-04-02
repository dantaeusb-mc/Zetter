package com.dantaeusb.zetter.client.gui.painting;

import com.dantaeusb.zetter.client.gui.PaintingScreen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.util.text.ITextComponent;

abstract public class AbstractPaintingWidget extends Widget {
    protected final PaintingScreen parentScreen;

    public AbstractPaintingWidget(PaintingScreen parentScreen, int x, int y, int width, int height, ITextComponent title) {
        super(x, y, width, height, title);

        this.parentScreen = parentScreen;
    }
}
