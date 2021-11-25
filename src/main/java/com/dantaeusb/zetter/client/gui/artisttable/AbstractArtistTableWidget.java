package com.dantaeusb.zetter.client.gui.artisttable;

import com.dantaeusb.zetter.client.gui.ArtistTableScreen;
import com.dantaeusb.zetter.client.gui.PaintingScreen;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.util.text.ITextComponent;

import javax.annotation.Nullable;

/**
 * @todo: Generic for both AbstractArtistTableWidget and AbstractPaintingWidget
 */
abstract public class AbstractArtistTableWidget extends Widget implements IGuiEventListener {
    protected final ArtistTableScreen parentScreen;

    public AbstractArtistTableWidget(ArtistTableScreen parentScreen, int x, int y, int width, int height, ITextComponent title) {
        super(x, y, width, height, title);

        this.parentScreen = parentScreen;
    }

    public @Nullable
    ITextComponent getTooltip(int mouseX, int mouseY) {
        return this.getMessage();
    }
}
