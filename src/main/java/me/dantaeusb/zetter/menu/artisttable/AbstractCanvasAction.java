package me.dantaeusb.zetter.menu.artisttable;

import me.dantaeusb.zetter.menu.ArtistTableMenu;
import me.dantaeusb.zetter.storage.DummyCanvasData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;

public abstract class AbstractCanvasAction {
    public State state;
    protected final ArtistTableMenu menu;
    protected final Level level;

    @Nullable
    protected DummyCanvasData canvasData;

    protected AbstractCanvasAction(ArtistTableMenu menu, Level level) {
        this.menu = menu;
        this.level = level;
    }

    public DummyCanvasData getCanvasData() {
        return this.canvasData;
    }

    public boolean mayPlaceGrid(int slot, ItemStack stack) {
        return false;
    }

    public void onChangeGrid(ItemStackHandler container) { }

    public void onTakeGrid(Player player, ItemStack stack) { }

    public boolean mayPlaceCombined(ItemStack stack) {
        return false;
    }

    public void onChangedCombined(ItemStackHandler container) { }

    public void onTakeCombined(Player player, ItemStack stack) { }

    public enum State {
        EMPTY,
        INVALID,
        NOT_LOADED,
        READY
    }
}
