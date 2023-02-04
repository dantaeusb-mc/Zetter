package me.dantaeusb.zetter.client.gui.artisttable;

import me.dantaeusb.zetter.client.gui.ArtistTableScreen;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.util.text.ITextComponent;

import javax.annotation.Nullable;

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
