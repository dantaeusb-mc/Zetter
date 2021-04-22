package com.dantaeusb.zetter.container;

import com.dantaeusb.zetter.container.artisttable.CanvasCombination;
import com.dantaeusb.zetter.core.ModContainers;
import com.dantaeusb.zetter.core.ModItems;
import com.dantaeusb.zetter.storage.CanvasData;
import com.dantaeusb.zetter.tileentity.ArtistTableTileEntity;
import com.dantaeusb.zetter.tileentity.storage.ArtistTableCanvasStorage;
import com.dantaeusb.zetter.tileentity.storage.ArtistTableFrameStorage;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.CraftResultInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Tuple;
import net.minecraft.world.World;

public class ArtistTableContainer extends Container {
    private static final int HOTBAR_SLOT_COUNT = 9;

    private static final int PLAYER_INVENTORY_ROW_COUNT = 3;
    private static final int PLAYER_INVENTORY_COLUMN_COUNT = 9;

    private static final int CANVAS_ROW_COUNT = 3;
    private static final int CANVAS_COLUMN_COUNT = 4;
    public static final int CANVAS_SLOT_COUNT = CANVAS_ROW_COUNT * CANVAS_COLUMN_COUNT;

    private final World world;

    private ArtistTableCanvasStorage canvasStorage;
    private ArtistTableFrameStorage frameStorage;

    private CanvasCombination canvasCombination;

    protected final CraftResultInventory inventoryOut = new CraftResultInventory();

    // gui position of the player inventory grid
    public static final int PLAYER_INVENTORY_XPOS = 8;
    public static final int PLAYER_INVENTORY_YPOS = 120;

    public ArtistTableContainer(int windowID, PlayerInventory invPlayer,
                                ArtistTableCanvasStorage canvasStorage,
                                ArtistTableFrameStorage frameStorage) {
        super(ModContainers.ARTIST_TABLE, windowID);

        if (ModContainers.ARTIST_TABLE == null)
            throw new IllegalStateException("Must initialise containerTypeArtistTableContainer before constructing a ArtistTableContainer!");

        this.world = invPlayer.player.world;

        this.canvasStorage = canvasStorage;
        this.frameStorage = frameStorage;

        this.canvasStorage.setMarkDirtyNotificationLambda(this::updateCanvasCombination);

        final int SLOT_X_SPACING = 18;
        final int SLOT_Y_SPACING = 18;
        final int HOTBAR_XPOS = 8;
        final int HOTBAR_YPOS = 178;

        // Add the players hotbar to the gui - the [xpos, ypos] location of each item
        for (int x = 0; x < HOTBAR_SLOT_COUNT; x++) {
            this.addSlot(new Slot(invPlayer, x, HOTBAR_XPOS + SLOT_X_SPACING * x, HOTBAR_YPOS));
        }

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
                this.addSlot(new SlotCanvas(this.canvasStorage, slotNumber,  xpos, ypos));
            }
        }

        // gui position of the player material slots
        final int FRAME_XPOS = 30;
        final int FRAME_YPOS = 15;

        this.addSlot(new SlotFrameMainMaterialInput(this.frameStorage, 0, FRAME_XPOS, FRAME_YPOS));
        this.addSlot(new SlotFrameDetailMaterialInput(this.frameStorage, 1, FRAME_XPOS + SLOT_X_SPACING, FRAME_YPOS));

        this.updateCanvasCombination();
    }

    public static ArtistTableContainer createContainerServerSide(int windowID, PlayerInventory playerInventory,
                                                                 ArtistTableCanvasStorage canvasStorage,
                                                                 ArtistTableFrameStorage frameStorage) {
        return new ArtistTableContainer(windowID, playerInventory, canvasStorage, frameStorage);
    }

    public static ArtistTableContainer createContainerClientSide(int windowID, PlayerInventory playerInventory, net.minecraft.network.PacketBuffer networkBuffer) {
        ArtistTableCanvasStorage canvasStorage = ArtistTableCanvasStorage.createForClientSideContainer();
        ArtistTableFrameStorage frameStorage = ArtistTableFrameStorage.createForClientSideContainer();

        return new ArtistTableContainer(windowID, playerInventory, canvasStorage, frameStorage);
    }

    public ArtistTableCanvasStorage getCanvasStorage() {
        return this.canvasStorage;
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

    private void updateCanvasCombination() {
        this.canvasCombination = new CanvasCombination(this.canvasStorage, this.world);
    }

    public CanvasCombination getCanvasCombination() {
        return this.canvasCombination;
    }

    public boolean canvasReady() {
        return this.canvasCombination.valid == CanvasCombination.State.READY;
    }

    public boolean canvasLoading() {
        return this.canvasCombination.valid == CanvasCombination.State.NOT_LOADED;
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

    public class SlotCanvas extends Slot {
        public SlotCanvas(IInventory inventoryIn, int index, int xPosition, int yPosition) {
            super(inventoryIn, index, xPosition, yPosition);
        }

        // if this function returns false, the player won't be able to insert the given item into this slot
        @Override
        public boolean isItemValid(ItemStack stack) {
            return ArtistTableTileEntity.isItemValidForCanvasArea(stack);
        }
    }

    public class SlotFrameMainMaterialInput extends Slot {
        public SlotFrameMainMaterialInput(IInventory inventoryIn, int index, int xPosition, int yPosition) {
            super(inventoryIn, index, xPosition, yPosition);
        }

        // if this function returns false, the player won't be able to insert the given item into this slot
        @Override
        public boolean isItemValid(ItemStack stack) {
            return ArtistTableTileEntity.isItemValidForFrameMainMaterial(stack);
        }
    }

    public class SlotFrameDetailMaterialInput extends Slot {
        public SlotFrameDetailMaterialInput(IInventory inventoryIn, int index, int xPosition, int yPosition) {
            super(inventoryIn, index, xPosition, yPosition);
        }

        // if this function returns false, the player won't be able to insert the given item into this slot
        @Override
        public boolean isItemValid(ItemStack stack) {
            return ArtistTableTileEntity.isItemValidForFrameDetailMaterial(stack);
        }
    }
}
