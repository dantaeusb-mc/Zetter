package com.dantaeusb.zetter.container;

import com.dantaeusb.zetter.container.artisttable.CanvasCombination;
import com.dantaeusb.zetter.core.Helper;
import com.dantaeusb.zetter.core.ModContainers;
import com.dantaeusb.zetter.core.ModItems;
import com.dantaeusb.zetter.item.FrameItem;
import com.dantaeusb.zetter.storage.DummyCanvasData;
import com.dantaeusb.zetter.storage.PaintingData;
import com.dantaeusb.zetter.tileentity.ArtistTableTileEntity;
import com.dantaeusb.zetter.tileentity.storage.ArtistTableCanvasStorage;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IWorldPosCallable;
import net.minecraft.world.World;
import org.apache.commons.lang3.StringUtils;

public class ArtistTableContainer extends Container {
    private static final int HOTBAR_SLOT_COUNT = 9;

    private static final int PLAYER_INVENTORY_ROW_COUNT = 3;
    private static final int PLAYER_INVENTORY_COLUMN_COUNT = 9;

    private static final int CANVAS_ROW_COUNT = 3;
    private static final int CANVAS_COLUMN_COUNT = 4;
    public static final int CANVAS_SLOT_COUNT = CANVAS_ROW_COUNT * CANVAS_COLUMN_COUNT;

    private final IWorldPosCallable worldPosCallable;

    private final PlayerEntity player;
    private final World world;

    private ArtistTableCanvasStorage canvasStorage;

    private CanvasCombination canvasCombination;

    private String paintingName = "";

    protected final Inventory inventoryOut = new Inventory(1);

    // gui position of the player inventory grid
    public static final int PLAYER_INVENTORY_XPOS = 8;
    public static final int PLAYER_INVENTORY_YPOS = 120;

    public ArtistTableContainer(int windowID, PlayerInventory invPlayer,
                                ArtistTableCanvasStorage canvasStorage,
                                final IWorldPosCallable worldPosCallable) {
        super(ModContainers.ARTIST_TABLE, windowID);

        this.worldPosCallable = worldPosCallable;

        this.player = invPlayer.player;
        this.world = invPlayer.player.world;

        this.canvasStorage = canvasStorage;
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
        final int CANVAS_INVENTORY_YPOS = 24;

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
        final int OUTPUT_XPOS = 152;
        final int OUTPUT_YPOS = 89;

        this.addSlot(new SlotOutput(this.inventoryOut, 0, OUTPUT_XPOS, OUTPUT_YPOS));

        this.updateCanvasCombination();
    }

    public static ArtistTableContainer createContainerServerSide(int windowID, PlayerInventory playerInventory,
                                                                 ArtistTableCanvasStorage canvasStorage,
                                                                 final IWorldPosCallable worldPosCallable) {
        return new ArtistTableContainer(windowID, playerInventory, canvasStorage, worldPosCallable);
    }

    public static ArtistTableContainer createContainerClientSide(int windowID, PlayerInventory playerInventory, net.minecraft.network.PacketBuffer networkBuffer) {
        ArtistTableCanvasStorage canvasStorage = ArtistTableCanvasStorage.createForClientSideContainer();

        return new ArtistTableContainer(windowID, playerInventory, canvasStorage, IWorldPosCallable.DUMMY);
    }

    public void updatePaintingName(String newPaintingName) {
        this.paintingName = newPaintingName;

        this.updatePaintingOutput();
    }

    /**
     * Called from network when player presses a button
     * @param authorPlayer
     * @param paintingName
     * @param clientCombinedCanvasData Canvas data used to check correctness of data
     */
    public void updatePaintingOutput() {
        ItemStack existingStack = this.inventoryOut.getStackInSlot(0);
        ItemStack outStack;

        if (this.isCanvasReady()) {
            if (existingStack.isEmpty()) {
                outStack = new ItemStack(ModItems.PAINTING);
            } else {
                outStack = existingStack;
            }
        } else {
            outStack = ItemStack.EMPTY;
        }

        if (!outStack.isEmpty()) {
            if (!StringUtils.isBlank(this.paintingName)) {
                if (existingStack.hasDisplayName()) {
                    existingStack.clearCustomName();
                }
            } else if (!this.paintingName.equals(FrameItem.getCachedAuthorName(outStack))) {
                FrameItem.setCachedPaintingName(outStack, this.paintingName);
            }

            final String authorName = this.player.getName().getString();
            if (!authorName.equals(FrameItem.getCachedAuthorName(outStack))) {
                FrameItem.setCachedAuthorName(outStack, authorName);
            }
        }

        this.inventoryOut.setInventorySlotContents(0, outStack);
    }

    protected ItemStack takePainting(PlayerEntity player, ItemStack outStack) {
        DummyCanvasData combinedCanvasData = this.getCanvasCombination().canvasData;
        PaintingData paintingData = Helper.createNewPainting(world, combinedCanvasData, player.getName().getString(), paintingName);

        FrameItem.setPaintingData(outStack, paintingData);
        FrameItem.setBlockSize(
                outStack,
                new int[]{
                        paintingData.getWidth() / paintingData.getResolution().getNumeric(),
                        paintingData.getHeight() / paintingData.getResolution().getNumeric()
                }
        );

        if (!player.isCreative()) {
            this.canvasStorage.clear();
        }

        return outStack;
    }

    public void updateCanvasCombination() {
        this.canvasCombination = new CanvasCombination(this.canvasStorage, this.world);
        this.updatePaintingOutput();
    }

    public CanvasCombination getCanvasCombination() {
        return this.canvasCombination;
    }

    public boolean isCanvasReady() {
        return this.canvasCombination.state == CanvasCombination.State.READY;
    }

    public boolean canvasLoading() {
        return this.canvasCombination.state == CanvasCombination.State.NOT_LOADED;
    }

    /*
      Common handlers
     */

    /**
     * Called to determine if the current slot is valid for the stack merging (double-click) code. The stack passed in is
     * null for the initial slot that was double-clicked.
     */
    public boolean canMergeSlot(ItemStack stack, Slot slotIn) {
        return slotIn.inventory != this.inventoryOut && super.canMergeSlot(stack, slotIn);
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
                if (sourceStack.getItem() == ModItems.PALETTE) {
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
    public boolean canInteractWith(PlayerEntity player) {
        return this.canvasStorage.isUsableByPlayer(player);
    }

    /**
     * Called when the container is closed.
     */
    public void onContainerClosed(PlayerEntity playerIn) {
        super.onContainerClosed(playerIn);

        this.worldPosCallable.consume((world, blockPos) -> {
            this.clearContainer(playerIn, playerIn.world, this.inventoryOut);
        });
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

    public class SlotFrameInput extends Slot {
        public SlotFrameInput(IInventory inventoryIn, int index, int xPosition, int yPosition) {
            super(inventoryIn, index, xPosition, yPosition);
        }

        // if this function returns false, the player won't be able to insert the given item into this slot
        @Override
        public boolean isItemValid(ItemStack stack) {
            return ArtistTableTileEntity.isItemValidForCanvasArea(stack);
        }
    }

    public class SlotOutput extends Slot {
        public SlotOutput(IInventory inventoryIn, int index, int xPosition, int yPosition) {
            super(inventoryIn, index, xPosition, yPosition);
        }

        @Override
        public boolean isItemValid(ItemStack stack) {
            return false;
        }

        public ItemStack onTake(PlayerEntity player, ItemStack stack) {
            return ArtistTableContainer.this.takePainting(player, stack);
        }
    }
}
