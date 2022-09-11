package me.dantaeusb.zetter.menu.artisttable;

import me.dantaeusb.zetter.block.entity.container.ArtistTableContainer;
import me.dantaeusb.zetter.core.ZetterItems;
import me.dantaeusb.zetter.menu.ArtistTableMenu;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.ItemStackHandler;

public class CanvasSplitAction extends AbstractCanvasAction {
    public CanvasSplitAction(ArtistTableMenu artistTableMenu, Level level) {
        super(artistTableMenu);
    }

    @Override
    public void containerChanged(ItemStackHandler container) {
        for (int y = 0; y < ArtistTableMenu.CANVAS_ROW_COUNT; y++) {
            for (int x = 0; x < ArtistTableMenu.CANVAS_COLUMN_COUNT; x++) {
                int slotNumber = y * ArtistTableMenu.CANVAS_COLUMN_COUNT + x;

                container.setStackInSlot(slotNumber, new ItemStack(ZetterItems.CANVAS.get()));
            }
        }
    }

    @Override
    public ItemStack onTake(Player player, ItemStack stack) {
        return stack;
    }
}
