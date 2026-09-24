package me.dantaeusb.zetter.entity.item.container;

import com.google.common.collect.Lists;
import me.dantaeusb.zetter.core.ItemStackHandlerListener;
import me.dantaeusb.zetter.entity.item.AbstractEaselEntity;
import me.dantaeusb.zetter.core.ZetterItems;
import me.dantaeusb.zetter.item.CanvasItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;

import java.util.List;


public class CanvasContainer extends ItemStackHandler {
    public static final int STORAGE_SIZE = 1;
    public static final int CANVAS_SLOT = 0;

    /**
     * Used when the container is not attached to a holder, see deprecated constructor
     */
    private static final int[] DEFAULT_MAX_CANVAS_BLOCK_SIZE = new int[]{2, 2};

    /*
     * Entity and listeners
     */

    private AbstractEaselEntity canvasHolder;
    private List<ItemStackHandlerListener> listeners;

    public CanvasContainer(AbstractEaselEntity easelEntity) {
        super(STORAGE_SIZE);

        this.canvasHolder = easelEntity;
    }

    @Deprecated
    public CanvasContainer() {
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


    /*
     * Validity
     */

    /**
     * @return
     */
    public boolean stillValid(Player player) {
        if (this.canvasHolder != null && this.canvasHolder.isAlive()) {
            return player.distanceToSqr(this.canvasHolder.getX() + 0.5D, this.canvasHolder.getY() + 0.5D, this.canvasHolder.getZ() + 0.5D) <= 64.0D;
        }

        return false;
    }

    public boolean isItemValid(int index, ItemStack stack) {
        if (index == CANVAS_SLOT && stack.getItem() == ZetterItems.CANVAS.get()) {
            int[] canvasSize = CanvasItem.getBlockSize(stack);
            assert canvasSize != null;

            int[] maxCanvasSize = this.canvasHolder == null
                ? DEFAULT_MAX_CANVAS_BLOCK_SIZE
                : this.canvasHolder.getMaxCanvasBlockSize();

            return canvasSize[0] <= maxCanvasSize[0] && canvasSize[1] <= maxCanvasSize[1];
        }

        return false;
    }

    /*
     * Getter-setters
     */
    public ItemStack getCanvasStack() {
        return this.getStackInSlot(CANVAS_SLOT);
    }

    /**
     * @return
     */
    public ItemStack extractCanvasStack() {
        return this.extractItem(CANVAS_SLOT, Integer.MAX_VALUE, false);
    }

    @Override
    public int getSlotLimit(int slot)
    {
        return 1;
    }

    public void setCanvasStack(ItemStack canvasStack) {
        this.setStackInSlot(CANVAS_SLOT, canvasStack);
    }

    public void changed() {
        this.onContentsChanged(CANVAS_SLOT);
    }

    /**
     * See {@link AbstractEaselEntity#containerChanged}.
     *
     * @param slot
     */
    @Override
    protected void onContentsChanged(int slot)
    {
        if (this.listeners != null) {
            for(ItemStackHandlerListener listener : this.listeners) {
                listener.containerChanged(this, slot);
            }
        }
    }
}