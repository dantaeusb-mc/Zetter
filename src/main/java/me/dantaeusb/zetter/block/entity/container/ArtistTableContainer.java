package me.dantaeusb.zetter.block.entity.container;

import com.google.common.collect.Lists;
import me.dantaeusb.zetter.block.entity.ArtistTableBlockEntity;
import me.dantaeusb.zetter.core.ItemStackHandlerListener;
import me.dantaeusb.zetter.core.ZetterItems;
import me.dantaeusb.zetter.menu.ArtistTableMenu;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;

import java.util.List;

public class ArtistTableContainer extends ItemStackHandler {
    public static final int STORAGE_SIZE = ArtistTableMenu.CANVAS_SLOT_COUNT;

    private ArtistTableBlockEntity boundTable;

    private List<ItemStackHandlerListener> listeners;

    public ArtistTableContainer(ArtistTableBlockEntity artistTable) {
        super(STORAGE_SIZE);

        this.boundTable = artistTable;
    }

    public ArtistTableContainer() {
        super(STORAGE_SIZE);
    }

    public void addListener(ItemStackHandlerListener listener) {
        if (this.listeners == null) {
            this.listeners = Lists.newArrayList();
        }

        this.listeners.add(listener);
    }

    public void removeListener(ItemStackHandlerListener listener) {
        this.listeners.remove(listener);
    }

    /**
     * @return
     */
    public boolean stillValid(Player player) {
        if (this.boundTable != null) {
            return player.distanceToSqr((double)this.boundTable.getBlockPos().getX() + 0.5D, (double)this.boundTable.getBlockPos().getY() + 0.5D, (double)this.boundTable.getBlockPos().getZ() + 0.5D) <= 64.0D;
        }

        // @todo: [LOW] False?
        return true;
    }

    public boolean isItemValid(int index, ItemStack stack) {
        return stack.getItem().equals(ZetterItems.CANVAS.get());
    }

    @Override
    public int getSlotLimit(int slot)
    {
        return 1;
    }

    public void clear() {
        this.stacks.clear();
    }

    protected void onContentsChanged(int slot)
    {
        if (this.listeners != null) {
            for(ItemStackHandlerListener listener : this.listeners) {
                listener.containerChanged(this);
            }
        }
    }
}