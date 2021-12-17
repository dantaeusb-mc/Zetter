package me.dantaeusb.zetter.entity.item.container;

import me.dantaeusb.zetter.core.ItemStackHandlerListener;
import me.dantaeusb.zetter.core.ZetterItems;
import me.dantaeusb.zetter.entity.item.EaselEntity;
import com.google.common.collect.Lists;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;

import java.util.List;


public class EaselContainer extends ItemStackHandler {
    public static final int STORAGE_SIZE = 2;

    public static final int CANVAS_SLOT = 0;
    public static final int PALETTE_SLOT = 1;

    private EaselEntity boundEasel;

    private List<ItemStackHandlerListener> listeners;

    public EaselContainer(EaselEntity easelEntity) {
        super(STORAGE_SIZE);

        this.boundEasel = easelEntity;
    }

    public EaselContainer() {
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
     * @todo: this
     * @return
     */
    public boolean stillValid(Player player) {
        if (this.boundEasel != null) {
            return player.distanceToSqr((double)this.boundEasel.getPos().getX() + 0.5D, (double)this.boundEasel.getPos().getY() + 0.5D, (double)this.boundEasel.getPos().getZ() + 0.5D) <= 64.0D;
        }

        return true;
    }

    public boolean isItemValid(int index, ItemStack stack) {
        return (index == 0 && stack.getItem() == ZetterItems.CANVAS)
                || (index == 1 && stack.getItem() == ZetterItems.PALETTE);
    }

    /**
     * @todo: move all interactions to TE
     * @return
     */
    public ItemStack getCanvasStack() {
        return this.getStackInSlot(CANVAS_SLOT);
    }

    public ItemStack getPaletteStack() {
        return this.getStackInSlot(PALETTE_SLOT);
    }

    public ItemStack extractCanvasStack() {
        return this.extractItem(CANVAS_SLOT, this.getSlotLimit(CANVAS_SLOT), false);
    }

    public ItemStack extractPaletteStack() {
        return this.extractItem(PALETTE_SLOT, this.getSlotLimit(PALETTE_SLOT), false);
    }

    @Override
    public int getSlotLimit(int slot)
    {
        return 1;
    }

    public void setCanvasStack(ItemStack canvasStack) {
        this.setStackInSlot(CANVAS_SLOT, canvasStack);
    }

    public void setPaletteStack(ItemStack canvasStack) {
        this.setStackInSlot(PALETTE_SLOT, canvasStack);
    }

    public void changed() {
        this.onContentsChanged(0);
    }

    protected void onContentsChanged(int slot)
    {
        if (this.listeners != null) {
            for(ItemStackHandlerListener listener : this.listeners) {
                listener.containerChanged(this);
            }
        }
    }

    /**
     * Conform to native Container interface
     */
}