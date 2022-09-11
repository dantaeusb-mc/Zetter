package me.dantaeusb.zetter.menu.artisttable;

import me.dantaeusb.zetter.menu.ArtistTableMenu;
import me.dantaeusb.zetter.storage.DummyCanvasData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;

public abstract class AbstractCanvasAction {
    public State state;
    protected final ArtistTableMenu menu;

    @Nullable
    protected DummyCanvasData canvasData;

    protected AbstractCanvasAction(ArtistTableMenu menu) {
        this.menu = menu;
    }

    public DummyCanvasData getCanvasData() {
        return this.canvasData;
    }

    public abstract ItemStack onTake(Player player, ItemStack stack);

    public abstract void containerChanged(ItemStackHandler container);

    public enum State {
        EMPTY,
        INVALID,
        NOT_LOADED,
        READY
    }
}
