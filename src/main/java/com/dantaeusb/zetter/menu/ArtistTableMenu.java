package com.dantaeusb.zetter.menu;

import com.dantaeusb.zetter.Zetter;
import com.dantaeusb.zetter.canvastracker.ICanvasTracker;
import com.dantaeusb.zetter.menu.artisttable.CanvasCombination;
import com.dantaeusb.zetter.core.Helper;
import com.dantaeusb.zetter.core.ModContainers;
import com.dantaeusb.zetter.core.ModItems;
import com.dantaeusb.zetter.item.FrameItem;
import com.dantaeusb.zetter.storage.DummyCanvasData;
import com.dantaeusb.zetter.storage.PaintingData;
import com.dantaeusb.zetter.block.entity.ArtistTableBlockEntity;
import com.dantaeusb.zetter.tileentity.container.ArtistTableCanvasStorage;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import org.apache.commons.lang3.StringUtils;

public class ArtistTableMenu extends AbstractContainerMenu {
    private static final int HOTBAR_SLOT_COUNT = 9;

    private static final int PLAYER_INVENTORY_ROW_COUNT = 3;
    private static final int PLAYER_INVENTORY_COLUMN_COUNT = 9;

    public static final int CANVAS_ROW_COUNT = 4;
    public static final int CANVAS_COLUMN_COUNT = 4;
    public static final int CANVAS_SLOT_COUNT = CANVAS_ROW_COUNT * CANVAS_COLUMN_COUNT;

    private final ContainerLevelAccess worldPosCallable;

    private final Player player;
    private final Level world;

    private ArtistTableCanvasStorage canvasStorage;

    private CanvasCombination canvasCombination;

    private String paintingName = "";

    protected final SimpleContainer inventoryOut = new SimpleContainer(1);

    // gui position of the player inventory grid
    public static final int PLAYER_INVENTORY_XPOS = 8;
    public static final int PLAYER_INVENTORY_YPOS = 138;

    public ArtistTableMenu(int windowID, Inventory invPlayer,
                           ArtistTableCanvasStorage canvasStorage,
                           final ContainerLevelAccess worldPosCallable) {
        super(ModContainers.ARTIST_TABLE, windowID);

        this.worldPosCallable = worldPosCallable;

        this.player = invPlayer.player;
        this.world = invPlayer.player.level;

        this.canvasStorage = canvasStorage;
        this.canvasStorage.setMarkDirtyNotificationLambda(this::updateCanvasCombination);

        final int SLOT_X_SPACING = 18;
        final int SLOT_Y_SPACING = 18;
        final int HOTBAR_XPOS = 8;
        final int HOTBAR_YPOS = 196;

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
        final int OUTPUT_YPOS = 107;

        this.addSlot(new SlotOutput(this.inventoryOut, 0, OUTPUT_XPOS, OUTPUT_YPOS));

        this.updateCanvasCombination();
    }

    public static ArtistTableMenu createContainerServerSide(int windowID, Inventory playerInventory,
                                                            ArtistTableCanvasStorage canvasStorage,
                                                            final ContainerLevelAccess worldPosCallable) {
        return new ArtistTableMenu(windowID, playerInventory, canvasStorage, worldPosCallable);
    }

    public static ArtistTableMenu createContainerClientSide(int windowID, Inventory playerInventory, net.minecraft.network.FriendlyByteBuf networkBuffer) {
        ArtistTableCanvasStorage canvasStorage = ArtistTableCanvasStorage.createForClientSideContainer();

        return new ArtistTableMenu(windowID, playerInventory, canvasStorage, ContainerLevelAccess.NULL);
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
        ItemStack existingStack = this.inventoryOut.getItem(0);
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
            if (StringUtils.isBlank(this.paintingName)) {
                if (existingStack.hasCustomHoverName()) {
                    existingStack.resetHoverName();
                }

                FrameItem.setCachedPaintingName(outStack, this.paintingName);
            } else if (!this.paintingName.equals(FrameItem.getCachedAuthorName(outStack))) {
                FrameItem.setCachedPaintingName(outStack, this.paintingName);
            }

            final String authorName = this.player.getName().getString();
            if (!authorName.equals(FrameItem.getCachedAuthorName(outStack))) {
                FrameItem.setCachedAuthorName(outStack, authorName);
            }
        }

        this.inventoryOut.setItem(0, outStack);
    }

    protected ItemStack takePainting(Player player, ItemStack outStack) {
        DummyCanvasData combinedCanvasData = this.getCanvasCombination().canvasData;
        ICanvasTracker canvasTracker = Helper.getWorldCanvasTracker(this.world);

        if (combinedCanvasData == null) {
            Zetter.LOG.error("Cannot find combined canvas data");
            return ItemStack.EMPTY;
        }

        /**
         * Feel like I'm getting ids before getting code always. Maybe make getCanvasCode call
         * CanvasTracker itself?
         */
        final int newId = canvasTracker.getNextPaintingId();
        final String newCode = PaintingData.getCanvasCode(newId);
        PaintingData paintingData = PaintingData.createFrom(combinedCanvasData);
        paintingData.setMetaProperties(player.getName().getString(), this.paintingName);
        canvasTracker.registerCanvasData(PaintingData.getPaintingCode(newId), paintingData);

        FrameItem.setPaintingData(outStack, newCode, paintingData);
        FrameItem.setBlockSize(
                outStack,
                new int[]{
                        paintingData.getWidth() / paintingData.getResolution().getNumeric(),
                        paintingData.getHeight() / paintingData.getResolution().getNumeric()
                }
        );

        if (!player.isCreative()) {
            this.canvasStorage.clearContent();
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
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slotIn) {
        return slotIn.container != this.inventoryOut && super.canTakeItemForPickAll(stack, slotIn);
    }

    /**
     *
     * @param playerIn
     * @param sourceSlotIndex
     * @return
     */
    @Override
    public ItemStack quickMoveStack(Player playerIn, int sourceSlotIndex)
    {
        ItemStack outStack = ItemStack.EMPTY;
        Slot sourceSlot = this.slots.get(sourceSlotIndex);

        if (sourceSlot != null && sourceSlot.hasItem()) {
            ItemStack sourceStack = sourceSlot.getItem();
            outStack = sourceStack.copy();

            // Palette
            if (sourceSlotIndex == 0) {
                if (!this.moveItemStackTo(sourceStack, 2, 10, true)) {
                    return ItemStack.EMPTY;
                }

                sourceSlot.onQuickCraft(sourceStack, outStack);

            // Inventory
            } else {
                if (sourceStack.getItem() == ModItems.PALETTE) {
                    if (!this.moveItemStackTo(sourceStack, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    return ItemStack.EMPTY;
                }
            }

            if (sourceStack.isEmpty()) {
                sourceSlot.set(ItemStack.EMPTY);
            } else {
                sourceSlot.setChanged();
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
    public boolean stillValid(Player player) {
        return this.canvasStorage.stillValid(player);
    }

    public class SlotCanvas extends Slot {
        public SlotCanvas(Container inventoryIn, int index, int xPosition, int yPosition) {
            super(inventoryIn, index, xPosition, yPosition);
        }

        // if this function returns false, the player won't be able to insert the given item into this slot
        @Override
        public boolean mayPlace(ItemStack stack) {
            return ArtistTableBlockEntity.isItemValidForCanvasArea(stack);
        }
    }

    public class SlotFrameInput extends Slot {
        public SlotFrameInput(Container inventoryIn, int index, int xPosition, int yPosition) {
            super(inventoryIn, index, xPosition, yPosition);
        }

        // if this function returns false, the player won't be able to insert the given item into this slot
        @Override
        public boolean mayPlace(ItemStack stack) {
            return ArtistTableBlockEntity.isItemValidForCanvasArea(stack);
        }
    }

    public class SlotOutput extends Slot {
        public SlotOutput(Container inventoryIn, int index, int xPosition, int yPosition) {
            super(inventoryIn, index, xPosition, yPosition);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        public void onTake(Player player, ItemStack stack) {
            stack = ArtistTableMenu.this.takePainting(player, stack);
        }
    }
}
