package me.dantaeusb.zetter.menu.artisttable;

import me.dantaeusb.zetter.core.ZetterItems;
import me.dantaeusb.zetter.menu.ArtistTableMenu;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.ItemStackHandler;

public class CanvasSplitAction extends AbstractCanvasAction {
    public CanvasSplitAction(ArtistTableMenu artistTableMenu, Level level) {
        super(artistTableMenu, level);
    }

    public boolean mayPlaceCombined(ItemStack stack) {
        if (stack.is(ZetterItems.CANVAS.get())) {
            // Check if not empty, not 1x1
            return true;
        }

        return false;
    }

    @Override
    public void onChangeGrid(ItemStackHandler container) {
        for (int y = 0; y < ArtistTableMenu.CANVAS_ROW_COUNT; y++) {
            for (int x = 0; x < ArtistTableMenu.CANVAS_COLUMN_COUNT; x++) {
                int slotNumber = y * ArtistTableMenu.CANVAS_COLUMN_COUNT + x;

                container.setStackInSlot(slotNumber, new ItemStack(ZetterItems.CANVAS.get()));
            }
        }
    }

    @Override
    public void onChangedCombined(ItemStackHandler container) {

    }

    public ItemStack onTake(Player player, ItemStack stack) {
        return stack;
    }
}
