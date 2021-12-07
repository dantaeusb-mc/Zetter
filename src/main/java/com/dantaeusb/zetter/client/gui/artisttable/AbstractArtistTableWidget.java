package com.dantaeusb.zetter.client.gui.artisttable;

import com.dantaeusb.zetter.client.gui.ArtistTableScreen;
import com.dantaeusb.zetter.client.gui.PaintingScreen;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;

/**
 * @todo: Generic for both AbstractArtistTableWidget and AbstractPaintingWidget
 */
abstract public class AbstractArtistTableWidget extends AbstractWidget implements GuiEventListener {
    protected final ArtistTableScreen parentScreen;

    public AbstractArtistTableWidget(ArtistTableScreen parentScreen, int x, int y, int width, int height, Component title) {
        super(x, y, width, height, title);

        this.parentScreen = parentScreen;
    }

    public @Nullable
    Component getTooltip(int mouseX, int mouseY) {
        return this.getMessage();
    }
}
