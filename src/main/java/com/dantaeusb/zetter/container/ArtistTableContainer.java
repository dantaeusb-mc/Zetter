package com.dantaeusb.zetter.container;

import com.dantaeusb.zetter.Zetter;
import com.dantaeusb.zetter.container.artisttable.CanvasCombination;
import com.dantaeusb.zetter.core.Helper;
import com.dantaeusb.zetter.core.ModContainers;
import com.dantaeusb.zetter.core.ModCraftingRecipes;
import com.dantaeusb.zetter.core.ModItems;
import com.dantaeusb.zetter.item.FrameItem;
import com.dantaeusb.zetter.storage.AbstractCanvasData;
import com.dantaeusb.zetter.tileentity.ArtistTableTileEntity;
import com.dantaeusb.zetter.tileentity.storage.ArtistTableCanvasStorage;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.CraftResultInventory;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.IWorldPosCallable;
import net.minecraft.world.World;

public class ArtistTableContainer extends Container {
    private static final int HOTBAR_SLOT_COUNT = 9;

    private static final int PLAYER_INVENTORY_ROW_COUNT = 3;
    private static final int PLAYER_INVENTORY_COLUMN_COUNT = 9;

    private static final int CANVAS_ROW_COUNT = 3;
    private static final int CANVAS_COLUMN_COUNT = 4;
    public static final int CANVAS_SLOT_COUNT = CANVAS_ROW_COUNT * CANVAS_COLUMN_COUNT;
    public static final int FRAME_SLOT_COUNT = 2;

    private final IWorldPosCallable worldPosCallable;

    private final World world;

    private ArtistTableCanvasStorage canvasStorage;

    private boolean frameReady = false;

    private final CraftingInventory frameInventory = new CraftingInventory(this, FRAME_SLOT_COUNT, 1) {
        public void markDirty() {
            super.markDirty();
            ArtistTableContainer.this.onCraftMatrixChanged(this);
        }
    };

    private CanvasCombination canvasCombination;

    protected final CraftResultInventory inventoryOut = new CraftResultInventory();

    // gui position of the player inventory grid
    public static final int PLAYER_INVENTORY_XPOS = 8;
    public static final int PLAYER_INVENTORY_YPOS = 127;

    public ArtistTableContainer(int windowID, PlayerInventory invPlayer,
                                ArtistTableCanvasStorage canvasStorage,
                                final IWorldPosCallable worldPosCallable) {
        super(ModContainers.ARTIST_TABLE, windowID);

        this.worldPosCallable = worldPosCallable;

        this.world = invPlayer.player.world;

        this.canvasStorage = canvasStorage;
        this.canvasStorage.setMarkDirtyNotificationLambda(this::updateCanvasCombination);

        final int SLOT_X_SPACING = 18;
        final int SLOT_Y_SPACING = 18;
        final int HOTBAR_XPOS = 8;
        final int HOTBAR_YPOS = 185;

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

        this.addSlot(new SlotFrameMainMaterialInput(this.frameInventory, 0, FRAME_XPOS, FRAME_YPOS));
        this.addSlot(new SlotFrameDetailMaterialInput(this.frameInventory, 1, FRAME_XPOS + SLOT_X_SPACING, FRAME_YPOS));

        // gui position of the player material slots
        final int OUTPUT_XPOS = 152;
        final int OUTPUT_YPOS = 99;

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
    /**
     * Called from network when player presses a button
     * @param authorPlayer
     * @param paintingName
     * @param originalCanvasData
     */
    public void createPainting(PlayerEntity authorPlayer, String paintingName, AbstractCanvasData originalCanvasData) {
        IRecipe<?> recipe = this.world.getRecipeManager().getRecipe(ModCraftingRecipes.FRAMING_RECIPE_TYPE, this.frameInventory, this.world).orElse(null);

        if (recipe == null) {
            Zetter.LOG.error("Received message to create painting but no frame recipe was found");
            return;
        }

        ItemStack outStack = recipe.getRecipeOutput().copy();
        FrameItem.copyCanvasData(outStack, originalCanvasData, this.world);
        FrameItem.setTitle(outStack, paintingName);
        FrameItem.setAuthor(outStack, authorPlayer.getName().getString());
        FrameItem.setBlockSize(
                outStack,
                new int[]{
                        originalCanvasData.getWidth() / Helper.CANVAS_TEXTURE_RESOLUTION,
                        originalCanvasData.getHeight() / Helper.CANVAS_TEXTURE_RESOLUTION
                });

        if (!authorPlayer.isCreative()) {
            for (int i = 0; i < FRAME_SLOT_COUNT; i++) {
                this.frameInventory.decrStackSize(i, 1);
            }

            this.canvasStorage.clear();
        }


        this.inventoryOut.setInventorySlotContents(0, outStack);
    }

    public void updateCanvasCombination() {
        this.canvasCombination = new CanvasCombination(this.canvasStorage, this.world);
    }

    public CanvasCombination getCanvasCombination() {
        return this.canvasCombination;
    }

    public boolean isFrameReady() {
        return this.frameReady;
    }

    public boolean isCanvasReady() {
        return this.canvasCombination.valid == CanvasCombination.State.READY;
    }

    public boolean canvasLoading() {
        return this.canvasCombination.valid == CanvasCombination.State.NOT_LOADED;
    }

    /*
      Common handlers
     */

    /**
     * Callback for when the crafting matrix is changed.
     */
    public void onCraftMatrixChanged(IInventory inventoryIn) {
        super.onCraftMatrixChanged(inventoryIn);

        if (inventoryIn == this.frameInventory) {
            IRecipe<?> recipe = this.world.getRecipeManager().getRecipe(ModCraftingRecipes.FRAMING_RECIPE_TYPE, this.frameInventory, this.world).orElse(null);

            if (recipe != null) {
                this.frameReady = true;
                return;
            }
        }

        this.frameReady = false;
    }

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
    public boolean canInteractWith(PlayerEntity playerIn) {
        return true;
        /*return this.worldPosCallable.applyOrElse((worldPosConsumer, defaultValue) -> {
            return !this.isAnEasel(worldPosConsumer.getBlockState(defaultValue)) ? false : playerIn.getDistanceSq((double)defaultValue.getX() + 0.5D, (double)defaultValue.getY() + 0.5D, (double)defaultValue.getZ() + 0.5D) <= 64.0D;
        }, true);*/
    }

    /**
     * Called when the container is closed.
     */
    public void onContainerClosed(PlayerEntity playerIn) {
        super.onContainerClosed(playerIn);

        this.worldPosCallable.consume((world, blockPos) -> {
            this.clearContainer(playerIn, playerIn.world, this.frameInventory);
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

    public class SlotOutput extends Slot {
        public SlotOutput(IInventory inventoryIn, int index, int xPosition, int yPosition) {
            super(inventoryIn, index, xPosition, yPosition);
        }

        // if this function returns false, the player won't be able to insert the given item into this slot
        @Override
        public boolean isItemValid(ItemStack stack) {
            return false;
        }
    }
}
