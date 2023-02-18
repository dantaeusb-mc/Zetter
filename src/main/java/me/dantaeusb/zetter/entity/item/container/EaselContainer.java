package me.dantaeusb.zetter.entity.item.container;

import me.dantaeusb.zetter.capability.canvastracker.CanvasTracker;
import me.dantaeusb.zetter.core.*;
import me.dantaeusb.zetter.entity.item.EaselEntity;
import com.google.common.collect.Lists;
import me.dantaeusb.zetter.item.CanvasItem;
import me.dantaeusb.zetter.storage.CanvasData;
import me.dantaeusb.zetter.storage.util.CanvasHolder;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.util.*;


public class EaselContainer extends ItemStackHandler implements IInventory {
    public static final int STORAGE_SIZE = 2;
    public static final int CANVAS_SLOT = 0;
    public static final int PALETTE_SLOT = 1;

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

    private EaselEntity easel;
    private List<ItemStackHandlerListener> listeners;

    public EaselContainer(EaselEntity easelEntity) {
        super(STORAGE_SIZE);

        this.easel = easelEntity;
    }

    @Deprecated
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

    /*
     * Palette
     */

    public void damagePalette(int damage) {
        final int maxDamage = this.getPaletteStack().getMaxDamage() - 1;
        int newDamage = this.getPaletteStack().getDamageValue() + damage;
        newDamage = Math.min(newDamage, maxDamage);

        this.getPaletteStack().setDamageValue(newDamage);
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

        CanvasTracker canvasTracker = Helper.getLevelCanvasTracker(this.easel.getLevel());
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
    public boolean stillValid(PlayerEntity player) {
        if (this.easel != null && this.easel.isAlive()) {
            return player.distanceToSqr((double)this.easel.getPos().getX() + 0.5D, (double)this.easel.getPos().getY() + 0.5D, (double)this.easel.getPos().getZ() + 0.5D) <= 64.0D;
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
        this.onContentsChanged(CANVAS_SLOT);
    }

    @Override
    public void clearContent() {
        this.stacks.clear();
        this.setChanged();
    }
}