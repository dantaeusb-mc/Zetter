package com.dantaeusb.zetter.tileentity.container;

import com.dantaeusb.zetter.core.ModItems;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;


public class EaselContainer extends SimpleContainer implements Container {
    public static final int STORAGE_SIZE = 2;

    public static final int CANVAS_SLOT = 0;
    public static final int PALETTE_SLOT = 1;

    public EaselContainer() {
        super(STORAGE_SIZE);
    }

    @Override
    public boolean canPlaceItem(int index, ItemStack stack) {
        return (index == 0 && stack.getItem() == ModItems.CANVAS)
                || (index == 1 && stack.getItem() == ModItems.PALETTE);
    }

    /**
     * @todo: move all interactions to TE
     * @return
     */
    public ItemStack getCanvasStack() {
        return this.getItem(CANVAS_SLOT);
    }

    public ItemStack getPaletteStack() {
        return this.getItem(PALETTE_SLOT);
    }

    public ItemStack extractCanvasStack() {
        return this.removeItem(CANVAS_SLOT, this.getMaxStackSize());
    }

    public ItemStack extractPaletteStack() {
        return this.removeItem(PALETTE_SLOT, this.getMaxStackSize());
    }

    public void setCanvasStack(ItemStack canvasStack) {
        this.setItem(CANVAS_SLOT, canvasStack);
    }

    public void setPaletteStack(ItemStack canvasStack) {
        this.setItem(PALETTE_SLOT, canvasStack);
    }
}