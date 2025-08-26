package me.dantaeusb.zetter.entity.item.container;

import com.google.common.collect.Lists;
import me.dantaeusb.zetter.capability.canvastracker.CanvasTracker;
import me.dantaeusb.zetter.core.Helper;
import me.dantaeusb.zetter.core.ItemStackHandlerListener;
import me.dantaeusb.zetter.core.ZetterItems;
import me.dantaeusb.zetter.entity.item.CanvasHolderEntity;
import me.dantaeusb.zetter.entity.item.EaselEntity;
import me.dantaeusb.zetter.item.CanvasItem;
import me.dantaeusb.zetter.storage.CanvasData;
import me.dantaeusb.zetter.storage.util.CanvasHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.util.List;


public class CanvasContainer extends ItemStackHandler {
    public static final int STORAGE_SIZE = 1;
    public static final int CANVAS_SLOT = 0;

    /*
     * Canvas
     *
     * Practically, this holder used to represent canvas
     * in Menu screen, because there's no data in item
     * on client side.
     *
     * Can reference default canvas on client only
     *
     * On server it should be synced with entity
     * data slot for the canvas code (we don't necessarily
     * load inventory for easel entity)
     */
    private @Nullable CanvasHolder<CanvasData> canvas;

    /*
     * Entity and listeners
     */

    private CanvasHolderEntity canvasHolder;
    private List<ItemStackHandlerListener> listeners;

    public CanvasContainer(CanvasHolderEntity easelEntity) {
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
     * Canvas
     */

    public CanvasHolder<CanvasData> getCanvas() {
        return this.canvas;
    }

    /**
     * When canvas code is changed in Easel entity's container
     *
     * Keep in mind that default canvases are only existing on client
     * logical side! Canvases like default_2x1 will not work on server
     * side and will be considered empty, which is actually useful
     * and it is how it's supposed to work
     *
     * @param canvasCode
     */
    public void handleCanvasChange(@Nullable String canvasCode) {
        if (canvasCode == null) {
            this.canvas = null;
            return;
        }

        CanvasTracker canvasTracker = Helper.getLevelCanvasTracker(this.canvasHolder.level());
        CanvasData canvas = canvasTracker.getCanvasData(canvasCode);

        if (canvas == null) {
            this.canvas = null;
            return;
        }

        this.canvas = new CanvasHolder<>(canvasCode, canvas);
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
        if (index == 0 && stack.getItem() == ZetterItems.CANVAS.get()) {
            int[] canvasSize = CanvasItem.getBlockSize(stack);
            assert canvasSize != null;

            return canvasSize[0] <= 2 && canvasSize[1] <= 2;
        }

        return index == 1 && stack.getItem() == ZetterItems.PALETTE.get();
    }

    /*
     * Getter-setters
     */
    public ItemStack getCanvasStack() {
        return this.getStackInSlot(CANVAS_SLOT);
    }

    public ItemStack extractCanvasStack() {
        return this.extractItem(CANVAS_SLOT, this.getSlotLimit(CANVAS_SLOT), false);
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

    @Override
    protected void onLoad() {
        this.handleCanvasChange(CanvasItem.getCanvasCode(this.getCanvasStack()));
    }

    @Override
    protected void onContentsChanged(int slot)
    {
        if (slot == CANVAS_SLOT) {
            ItemStack canvasStack = this.getCanvasStack();

            if (canvasStack.isEmpty()) {
                this.handleCanvasChange(null);
            } else {
                String canvasCode = CanvasItem.getCanvasCode(canvasStack);

                if (canvasCode == null) {
                    int[] size = CanvasItem.getBlockSize(canvasStack);
                    assert size != null && size.length == 2;

                    canvasCode = CanvasData.getDefaultCanvasCode(size[0], size[1]);
                }

                this.handleCanvasChange(canvasCode);
            }
        }

        if (this.listeners != null) {
            for(ItemStackHandlerListener listener : this.listeners) {
                listener.containerChanged(this, slot);
            }
        }
    }
}