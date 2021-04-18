package com.dantaeusb.zetter.container;

import com.dantaeusb.zetter.core.ModContainers;
import com.dantaeusb.zetter.core.ModItems;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.CraftResultInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IWorldPosCallable;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.vector.Vector3i;
import net.minecraft.world.World;

import java.awt.geom.Rectangle2D;
import java.util.Vector;

public class ArtistTableContainer extends Container {
    // @todo: move to helper
    public static final int[][] paintingShapes = new int[][]{
            {1, 1},
            {1, 2},
            {2, 1},
            {2, 3},
            {3, 3},
            {4, 2},
            {4, 3}
    };

    private final PlayerEntity player;
    private final World world;

    private static final int HOTBAR_SLOT_COUNT = 9;

    private static final int PLAYER_INVENTORY_ROW_COUNT = 3;
    private static final int PLAYER_INVENTORY_COLUMN_COUNT = 9;

    private static final int CANVAS_ROW_COUNT = 3;
    private static final int CANVAS_COLUMN_COUNT = 4;
    private static final int CANVAS_SLOT_COUNT = CANVAS_ROW_COUNT * CANVAS_COLUMN_COUNT;

    private static final int FRAME_SLOTS_COUNT = 2;

    protected final IWorldPosCallable worldPosCallable;

    protected final IInventory inventoryCanvas = new Inventory(CANVAS_SLOT_COUNT) {
        /**
         * @see net.minecraft.inventory.container.RepairContainer#field_234643_d_
         */
        public void markDirty() {
            super.markDirty();
            ArtistTableContainer.this.onCraftMatrixChanged(this);
        }
    };

    protected final IInventory inventoryFrame = new Inventory(FRAME_SLOTS_COUNT) {
        /**
         * @see net.minecraft.inventory.container.RepairContainer#field_234643_d_
         */
        public void markDirty() {
            super.markDirty();
            ArtistTableContainer.this.onCraftMatrixChanged(this);
        }
    };

    protected final CraftResultInventory inventoryOut = new CraftResultInventory();

    public ArtistTableContainer(int windowID, PlayerInventory playerInventory) {
        this(windowID, playerInventory, IWorldPosCallable.DUMMY);
    }

    public ArtistTableContainer(int windowID, PlayerInventory invPlayer, IWorldPosCallable worldPosCallable) {
        super(ModContainers.ARTIST_TABLE, windowID);

        if (ModContainers.ARTIST_TABLE == null)
            throw new IllegalStateException("Must initialise containerTypeArtistTableContainer before constructing a ArtistTableContainer!");

        this.player = invPlayer.player;
        this.world = invPlayer.player.world;
        this.worldPosCallable = worldPosCallable;

        final int SLOT_X_SPACING = 18;
        final int SLOT_Y_SPACING = 18;
        final int HOTBAR_XPOS = 8;
        final int HOTBAR_YPOS = 178;

        // Add the players hotbar to the gui - the [xpos, ypos] location of each item
        for (int x = 0; x < HOTBAR_SLOT_COUNT; x++) {
            this.addSlot(new Slot(invPlayer, x, HOTBAR_XPOS + SLOT_X_SPACING * x, HOTBAR_YPOS));
        }

        // gui position of the player inventory grid
        final int PLAYER_INVENTORY_XPOS = 8;
        final int PLAYER_INVENTORY_YPOS = 120;

        // Add the rest of the players inventory to the gui
        for (int y = 0; y < PLAYER_INVENTORY_ROW_COUNT; y++) {
            for (int x = 0; x < PLAYER_INVENTORY_COLUMN_COUNT; x++) {
                int slotNumber = HOTBAR_SLOT_COUNT + y * PLAYER_INVENTORY_COLUMN_COUNT + x;
                int xpos = PLAYER_INVENTORY_XPOS + x * SLOT_X_SPACING;
                int ypos = PLAYER_INVENTORY_YPOS + y * SLOT_Y_SPACING;
                this.addSlot(new Slot(invPlayer, slotNumber,  xpos, ypos));
            }
        }

        // gui position of the player inventory grid
        final int CANVAS_INVENTORY_XPOS = 12;
        final int CANVAS_INVENTORY_YPOS = 39;

        // Add canvas sewing slots
        for (int y = 0; y < CANVAS_ROW_COUNT; y++) {
            for (int x = 0; x < CANVAS_COLUMN_COUNT; x++) {
                int slotNumber = y * CANVAS_COLUMN_COUNT + x;
                int xpos = CANVAS_INVENTORY_XPOS + x * SLOT_X_SPACING;
                int ypos = CANVAS_INVENTORY_YPOS + y * SLOT_Y_SPACING;
                this.addSlot(new Slot(this.inventoryCanvas, slotNumber,  xpos, ypos));
            }
        }

        // gui position of the player material slots
        final int FRAME_XPOS = 30;
        final int FRAME_YPOS = 15;

        // Add the frame crafting material slots
        for (int x = 0; x < FRAME_SLOTS_COUNT; x++) {
            this.addSlot(new Slot(this.inventoryFrame, x, FRAME_XPOS + SLOT_X_SPACING * x, FRAME_YPOS));
        }
    }

    public static ArtistTableContainer createContainerServerSide(int windowID, PlayerInventory playerInventory) {
        return new ArtistTableContainer(windowID, playerInventory);
    }

    public static ArtistTableContainer createContainerClientSide(int windowID, PlayerInventory playerInventory, net.minecraft.network.PacketBuffer networkBuffer) {
        return new ArtistTableContainer(windowID, playerInventory);
    }

    /*
      Common handlers
     */

    /**
     * Called when the container is closed.
     * Push painting frames so it will be saved
     */
    public void onContainerClosed(PlayerEntity playerIn) {
        super.onContainerClosed(playerIn);

    }

    /**
     *
     * @param playerIn
     * @param sourceSlotIndex
     * @return
     */
    @Override
    public ItemStack transferStackInSlot(PlayerEntity playerIn, int sourceSlotIndex)
    {
        ItemStack outStack = ItemStack.EMPTY;
        Slot sourceSlot = this.inventorySlots.get(sourceSlotIndex);

        if (sourceSlot != null && sourceSlot.getHasStack()) {
            ItemStack sourceStack = sourceSlot.getStack();
            outStack = sourceStack.copy();

            // Palette
            if (sourceSlotIndex == 0) {
                if (!this.mergeItemStack(sourceStack, 2, 10, true)) {
                    return ItemStack.EMPTY;
                }

                sourceSlot.onSlotChange(sourceStack, outStack);

            // Inventory
            } else {
                if (sourceStack.getItem() == ModItems.PALETTE_ITEM) {
                    if (!this.mergeItemStack(sourceStack, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    return ItemStack.EMPTY;
                }
            }

            if (sourceStack.isEmpty()) {
                sourceSlot.putStack(ItemStack.EMPTY);
            } else {
                sourceSlot.onSlotChanged();
            }

            if (sourceStack.getCount() == outStack.getCount()) {
                return ItemStack.EMPTY;
            }

            sourceSlot.onTake(playerIn, sourceStack);
        }

        return outStack;
    }

    /**
     * Determines whether supplied player can use this container
     */
    public boolean canInteractWith(PlayerEntity playerIn) {
        return true;
        /*return this.worldPosCallable.applyOrElse((worldPosConsumer, defaultValue) -> {
            return !this.isAnEasel(worldPosConsumer.getBlockState(defaultValue)) ? false : playerIn.getDistanceSq((double)defaultValue.getX() + 0.5D, (double)defaultValue.getY() + 0.5D, (double)defaultValue.getZ() + 0.5D) <= 64.0D;
        }, true);*/
    }

    public boolean checkCanvasLayout() {
        Tuple<Integer, Integer> min = null;
        Tuple<Integer, Integer> max = null;

        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 4; x++) {
                if (this.inventoryCanvas.getStackInSlot(y * 4 + x) != ItemStack.EMPTY) {
                    if (min == null) {
                        min = new Tuple<>(x ,y);
                    }

                    max = new Tuple<>(x ,y);
                }
            }
        }

        if (min == null || max == null) {
            return false;
        }

        int length = max.getA() + 1 - min.getA();
        int height = max.getB() + 1 - min.getB();

        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 4; x++) {
                if (this.inventoryCanvas.getStackInSlot(y * 4 + x) == ItemStack.EMPTY) {
                    if (x >= min.getA() && x <= max.getA()) {
                        if (y >= min.getB() && y <= max.getB()) {
                            return false;
                        }
                    }
                }
            }
        }

        boolean shapeAvailable = false;
        for (int[] shape: ArtistTableContainer.paintingShapes) {
            if (length == shape[0] && height == shape[1]) {
                shapeAvailable = true;
            }
        }

        return shapeAvailable;
    }

    public IInventory getInventoryCanvas() {
        return this.inventoryCanvas;
    }

    public IInventory getInventoryFrame() {
        return this.inventoryFrame;
    }
}
