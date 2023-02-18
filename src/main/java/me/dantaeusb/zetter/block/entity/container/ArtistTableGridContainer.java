package me.dantaeusb.zetter.block.entity.container;

import com.google.common.collect.Lists;
import me.dantaeusb.zetter.block.entity.ArtistTableBlockEntity;
import me.dantaeusb.zetter.core.ItemStackHandlerListener;
import me.dantaeusb.zetter.core.ZetterItems;
import me.dantaeusb.zetter.item.CanvasItem;
import me.dantaeusb.zetter.menu.ArtistTableMenu;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;

import java.util.List;

public class ArtistTableGridContainer extends ItemStackHandler implements IInventory {
    public static final int STORAGE_SIZE = ArtistTableMenu.CANVAS_SLOT_COUNT;

    private ArtistTableBlockEntity boundTable;

    private List<ItemStackHandlerListener> listeners;

    public ArtistTableGridContainer(ArtistTableBlockEntity artistTable) {
        super(STORAGE_SIZE);

        this.boundTable = artistTable;
    }

    public ArtistTableGridContainer() {
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
    public boolean stillValid(PlayerEntity player) {
        if (this.boundTable != null) {
            return player.distanceToSqr((double)this.boundTable.getBlockPos().getX() + 0.5D, (double)this.boundTable.getBlockPos().getY() + 0.5D, (double)this.boundTable.getBlockPos().getZ() + 0.5D) <= 64.0D;
        }

        // @todo: [LOW] False?
        return true;
    }

    /**
     * All non-compound canvases are valid.
     * Compound canvases are invalid for grid,
     * they can be only split.
     *
     * @param index    Slot to query for validity
     * @param stack   Stack to test with for validity
     *
     * @return
     */
    public boolean isItemValid(int index, ItemStack stack) {
        return  stack.getItem().equals(ZetterItems.CANVAS.get()) &&
                !CanvasItem.isCompound(stack);
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
                listener.containerChanged(this, slot);
            }
        }
    }

    /*
     * IInventory things
     */

    @Override
    public int getContainerSize() {
        return STORAGE_SIZE;
    }

    @Override
    public boolean isEmpty() {
        for(ItemStack itemstack : this.stacks) {
            if (!itemstack.isEmpty()) {
                return false;
            }
        }

        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot >= 0 && slot < this.stacks.size() ? this.stacks.get(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        ItemStack itemstack = ItemStackHelper.removeItem(this.stacks, slot, count);
        if (!itemstack.isEmpty()) {
            this.setChanged();
        }

        return itemstack;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack itemstack = this.stacks.get(slot);

        if (itemstack.isEmpty()) {
            return ItemStack.EMPTY;
        } else {
            this.stacks.set(slot, ItemStack.EMPTY);
            return itemstack;
        }
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        this.stacks.set(slot, stack);
        if (!stack.isEmpty() && stack.getCount() > this.getMaxStackSize()) {
            stack.setCount(this.getMaxStackSize());
        }

        this.setChanged();
    }

    @Override
    public void setChanged() {
        for (int slot = 0; slot < this.stacks.size(); slot++) {
            this.onContentsChanged(slot);
        }
    }

    @Override
    public void clearContent() {
        this.stacks.clear();
        this.setChanged();
    }
}