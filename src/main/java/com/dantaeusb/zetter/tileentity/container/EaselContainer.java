package com.dantaeusb.zetter.tileentity.container;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;


public class EaselContainer extends SimpleContainer implements Container {
    public static final int STORAGE_SIZE = 2;

    public static final int CANVAS_SLOT = 0;
    public static final int PALETTE_SLOT = 1;

    public EaselContainer() {
        super(STORAGE_SIZE);
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
