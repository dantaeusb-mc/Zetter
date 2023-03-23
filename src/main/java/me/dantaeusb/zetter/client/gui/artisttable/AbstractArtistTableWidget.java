package me.dantaeusb.zetter.client.gui.artisttable;

import me.dantaeusb.zetter.client.gui.ArtistTableScreen;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;

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
