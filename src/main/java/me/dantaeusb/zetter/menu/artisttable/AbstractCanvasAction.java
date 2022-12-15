package me.dantaeusb.zetter.menu.artisttable;

import me.dantaeusb.zetter.menu.ArtistTableMenu;
import me.dantaeusb.zetter.storage.CanvasData;
import me.dantaeusb.zetter.storage.DummyCanvasData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;

public abstract class AbstractCanvasAction {
    public State state = State.EMPTY;
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

    public void onChangedCombination(ItemStackHandler container) { }

    public void onTakeCombination(Player player, ItemStack stack) { }

    public boolean mayPlaceCombined(ItemStack stack) {
        return false;
    }

    public void onChangedCombined(ItemStackHandler container) { }

    public void onTakeCombined(Player player, ItemStack stack) { }

    public void onChangedSplit(ItemStackHandler container) { }

    public void onTakeSplit(Player player, ItemStack stack) { }

    public void handleCanvasSync(String canvasCode, CanvasData canvasData, long timestamp) { }

    public enum State {
        EMPTY, // No items in crafting slots
        INVALID, // Invalid shape or canvas found, cannot proceed
        NOT_LOADED, // Canvases data is not yet loaded
        READY // Free to proceed with action
    }
}
