package me.dantaeusb.zetter.menu;

import me.dantaeusb.zetter.block.entity.container.ArtistTableGridContainer;
import me.dantaeusb.zetter.core.ItemStackHandlerListener;
import me.dantaeusb.zetter.core.ZetterContainerMenus;
import me.dantaeusb.zetter.core.ZetterNetwork;
import me.dantaeusb.zetter.menu.artisttable.AbstractCanvasAction;
import me.dantaeusb.zetter.menu.artisttable.CanvasCombinationAction;
import me.dantaeusb.zetter.core.ZetterItems;
import me.dantaeusb.zetter.mixin.SlotAccessor;
import me.dantaeusb.zetter.network.packet.CArtistTableModeChange;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

import javax.annotation.Nullable;
import java.util.ArrayList;

public class ArtistTableMenu extends AbstractContainerMenu implements ItemStackHandlerListener {
    private static final int HOTBAR_SLOT_COUNT = 9;

    private static final int PLAYER_INVENTORY_ROW_COUNT = 3;
    private static final int PLAYER_INVENTORY_COLUMN_COUNT = 9;

    public static final int CANVAS_ROW_COUNT = 4;
    public static final int CANVAS_COLUMN_COUNT = 4;
    public static final int CANVAS_SLOT_COUNT = CANVAS_ROW_COUNT * CANVAS_COLUMN_COUNT;

    private final Player player;
    private final Level level;

    private final ArrayList<Slot> gridSlots = new ArrayList<>(16);
    private final Slot combinedSlot;

    private AbstractCanvasAction action;

    private Mode mode = Mode.COMBINE;

    private final ArtistTableGridContainer gridContainer;
    protected final ItemStackHandler combinedContainer = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            ArtistTableMenu.this.combinedSlotChanged(this);
            super.onContentsChanged(slot);
            //super.onContentsChanged(slot);
        }
    };

    // gui position of the player inventory grid
    public static final int PLAYER_INVENTORY_XPOS = 36;
    public static final int PLAYER_INVENTORY_YPOS = 110;

    public static final int COMBINATION_SLOTS_COMBINE_X = 14;
    public static final int COMBINATION_SLOTS_COMBINE_Y = 20;

    public static final int COMBINATION_SLOTS_SPLIT_X = 230 - 18 * 4 - 14;
    public static final int COMBINATION_SLOTS_SPLIT_Y = 20;

    public static final int COMBINED_SLOT_X = 107;
    public static final int COMBINED_SLOT_Y = 67;

    final int SLOT_X_SPACING = 18;
    final int SLOT_Y_SPACING = 18;

    public ArtistTableMenu(int windowID, Inventory invPlayer,
                           ArtistTableGridContainer artistTableContainer) {
        super(ZetterContainerMenus.ARTIST_TABLE.get(), windowID);

        this.player = invPlayer.player;
        this.level = invPlayer.player.getLevel();

        this.gridContainer = artistTableContainer;
        this.gridContainer.addListener(this);

        final int HOTBAR_XPOS = 36;
        final int HOTBAR_YPOS = 168;

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

        // Add canvas sewing slots
        for (int y = 0; y < CANVAS_ROW_COUNT; y++) {
            for (int x = 0; x < CANVAS_COLUMN_COUNT; x++) {
                int slotNumber = y * CANVAS_COLUMN_COUNT + x;
                int xpos = COMBINATION_SLOTS_COMBINE_X + x * SLOT_X_SPACING;
                int ypos = COMBINATION_SLOTS_COMBINE_Y + y * SLOT_Y_SPACING;

                final SlotCombinationGrid combinationSlot = new SlotCombinationGrid(this.gridContainer, slotNumber,  xpos, ypos);

                this.addSlot(combinationSlot);
                this.gridSlots.add(combinationSlot);
            }
        }

        final SlotCombined combinedSlot = new SlotCombined(this.combinedContainer, 0, COMBINED_SLOT_X, COMBINED_SLOT_Y);

        this.addSlot(combinedSlot);
        this.combinedSlot = combinedSlot;

        this.action = new CanvasCombinationAction(this, level);

        this.containerChanged(this.gridContainer);
    }

    public static ArtistTableMenu createMenuServerSide(int windowID, Inventory playerInventory,
                                                       ArtistTableGridContainer artistTableContainer) {
        return new ArtistTableMenu(windowID, playerInventory, artistTableContainer);
    }

    public static ArtistTableMenu createMenuClientSide(int windowID, Inventory playerInventory, net.minecraft.network.FriendlyByteBuf networkBuffer) {
        ArtistTableGridContainer artistTableContainer = new ArtistTableGridContainer();

        return new ArtistTableMenu(windowID, playerInventory, artistTableContainer);
    }

    public ItemStackHandler getGridContainer() {
        return this.gridContainer;
    }

    public ItemStackHandler getCombinedContainer() {
        return this.combinedContainer;
    }

    public Mode getMode() {
        return this.mode;
    }

    public void setMode(Mode mode) {
        if (!this.canChangeMode()) {
            return;
        }

        this.mode = mode;

        if (this.player.getLevel().isClientSide()) {
            CArtistTableModeChange unloadPacket = new CArtistTableModeChange(this.containerId, mode);
            ZetterNetwork.simpleChannel.sendToServer(unloadPacket);
        }

        this.updateSlotPositions();
    }

    public boolean canChangeMode() {
        if (this.getMode() == Mode.COMBINE) {
            return this.isCombinationGridEmpty();
        } else {
            return this.isCombinedSlotEmpty();
        }
    }

    public void updateSlotPositions() {
        final int firstIndex = this.gridSlots.get(0).getSlotIndex();

        final int combinationBaseX = this.mode == Mode.COMBINE ? COMBINATION_SLOTS_COMBINE_X : COMBINATION_SLOTS_SPLIT_X;
        final int combinationBaseY = this.mode == Mode.COMBINE ? COMBINATION_SLOTS_COMBINE_Y : COMBINATION_SLOTS_SPLIT_Y;

        for (Slot combinationSlot : this.gridSlots) {
            int x = (combinationSlot.getSlotIndex() - firstIndex) % CANVAS_ROW_COUNT;
            int y = (combinationSlot.getSlotIndex() - firstIndex) / CANVAS_COLUMN_COUNT;

            ((SlotAccessor) combinationSlot).setX(combinationBaseX + x * SLOT_X_SPACING);
            ((SlotAccessor) combinationSlot).setY(combinationBaseY + y * SLOT_Y_SPACING);
        }
    }

    public boolean isCombinationGridEmpty() {
        for (Slot combinationSlot : this.gridSlots) {
            if (combinationSlot.hasItem()) {
                return false;
            }
        }

        return true;
    }

    public boolean isCombinedSlotEmpty() {
        return !this.combinedSlot.hasItem();
    }

    /**
     * Update contents of painting when some
     * of the input parameters changed (i.e. name, canvases)
     *
     * In combination mode, we have the result
     * ONLY when player picks canvas up
     * from the combination slot
     *
     * But in split mode, we could have had complex
     * canvas in the slot already, so we should not
     * remove or overwrite it
     */
    public void containerChanged(ItemStackHandler container) {
        this.action.onChangeGrid(container);
    }

    public void combinedSlotChanged(ItemStackHandler combinedHandler) {
        this.action.onChangedCombined(combinedHandler);
    }

    public ItemStack onTakeFromGrid(Player player, ItemStack stack) {
        return stack;
    }

    public ItemStack onTakeFromCombinedSlot(Player player, ItemStack stack) {
        return stack;
    }

    public AbstractCanvasAction getAction() {
        return this.action;
    }

    public boolean isCanvasReady() {
        return this.action.state == CanvasCombinationAction.State.READY;
    }

    public boolean canvasLoading() {
        return this.action.state == CanvasCombinationAction.State.NOT_LOADED;
    }

    /*
      Common handlers
     */

    public void removed(Player player) {
        super.removed(player);
        this.gridContainer.removeListener(this);
    }

    /**
     * Called to determine if the current slot is valid for the stack merging (double-click) code. The stack passed in is
     * null for the initial slot that was double-clicked.
     */
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slotIn) {
        return slotIn.container != this.combinedContainer && super.canTakeItemForPickAll(stack, slotIn);
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
                if (sourceStack.getItem() == ZetterItems.PALETTE.get()) {
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
        return this.gridContainer.stillValid(player);
    }

    public class SlotCombinationGrid extends SlotItemHandler {
        public SlotCombinationGrid(ItemStackHandler stackHandler, int index, int xPosition, int yPosition) {
            super(stackHandler, index, xPosition, yPosition);
        }

        // if this function returns false, the player won't be able to insert the given item into this slot
        @Override
        public boolean mayPlace(ItemStack stack) {
            return ArtistTableMenu.this.action.mayPlaceGrid(this.getSlotIndex(), stack);
        }

        @Override
        public void onTake(Player player, ItemStack stack) {
            ArtistTableMenu.this.action.onTakeGrid(player, stack);
        }
    }

    public class SlotCombined extends SlotItemHandler {
        public SlotCombined(ItemStackHandler stackHandler, int index, int xPosition, int yPosition) {
            super(stackHandler, index, xPosition, yPosition);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return ArtistTableMenu.this.action.mayPlaceCombined(stack);
        }

        @Override
        public void onTake(Player player, ItemStack stack) {
            ArtistTableMenu.this.action.onTakeCombined(player, stack);
        }
    }

    public enum Mode {
        COMBINE(0),
        SPLIT(1);

        private final byte id;

        Mode(int id)
        {
            this.id = (byte) id;
        }

        public byte getId() {
            return this.id;
        }

        public static @Nullable ArtistTableMenu.Mode getById(byte id) {
            for (ArtistTableMenu.Mode mode : ArtistTableMenu.Mode.values()) {
                if (mode.id == id) {
                    return mode;
                }
            }

            return null;
        }
    }
}
