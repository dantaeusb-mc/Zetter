package me.dantaeusb.zetter.menu.artisttable;

import me.dantaeusb.zetter.core.ZetterItems;
import me.dantaeusb.zetter.item.CanvasItem;
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

    public void forEveryGridSlot(GridSlotCallback callback) {
        ItemStackHandler gridContainer = this.menu.getGridContainer();

        for (int y = 0; y < ArtistTableMenu.CANVAS_ROW_COUNT; y++) {
            for (int x = 0; x < ArtistTableMenu.CANVAS_COLUMN_COUNT; x++) {
                int slotNumber = y * ArtistTableMenu.CANVAS_COLUMN_COUNT + x;
                ItemStack stack = gridContainer.getStackInSlot(slotNumber);

                callback.call(gridContainer, x, y, stack, slotNumber);
            }
        }
    }

    @FunctionalInterface
    public interface GridSlotCallback {
        void call(ItemStackHandler container, int x, int y, ItemStack stack, int slotFinder);
    }

    public enum State {
        EMPTY,
        INVALID,
        NOT_LOADED,
        READY
    }
}
