package me.dantaeusb.zetter.menu;

import me.dantaeusb.zetter.block.entity.ArtistTableBlockEntity;
import me.dantaeusb.zetter.block.entity.container.ArtistTableGridContainer;
import me.dantaeusb.zetter.core.*;
import me.dantaeusb.zetter.item.CanvasItem;
import me.dantaeusb.zetter.menu.artisttable.AbstractCanvasAction;
import me.dantaeusb.zetter.menu.artisttable.CanvasCombinationAction;
import me.dantaeusb.zetter.menu.artisttable.CanvasSplitAction;
import me.dantaeusb.zetter.network.packet.CArtistTableModeChange;
import me.dantaeusb.zetter.network.packet.SArtistTableMenuCreatePacket;
import me.dantaeusb.zetter.storage.CanvasData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;

import static me.dantaeusb.zetter.block.entity.ArtistTableBlockEntity.DATA_MODE;

/**
 * @todo: [MID] This whole thing with ContainerData is not very smart
 * but at least we have single source of truth and transparent sync
 * with data slots
 */
public class ArtistTableMenu extends AbstractContainerMenu implements ItemStackHandlerListener, ContainerListener {
    private static final int HOTBAR_SLOT_COUNT = 9;

    private static final int PLAYER_INVENTORY_ROW_COUNT = 3;
    private static final int PLAYER_INVENTORY_COLUMN_COUNT = 9;

    public static final int CANVAS_ROW_COUNT = 4;
    public static final int CANVAS_COLUMN_COUNT = 4;
    public static final int CANVAS_SLOT_COUNT = CANVAS_ROW_COUNT * CANVAS_COLUMN_COUNT;

    private final Player player;
    private final Level level;

    /**
     * Three containers:
     * a. Combination - saved in the world, shown only in combination mode,
     *  for the purpose of making larger canvases from small (1x1) ones
     * b. Split - only in menu, shown only in split mode, for the
     * purpose of showing and handling small canvases when
     * player splits large canvas. If menu closed, dropped
     * or given to the player
     * c. Combined - always shown, in combination mode shows result,
     * in split mode used as a resource
     */

    private final ArrayList<Slot> gridSlots = new ArrayList<>(ArtistTableMenu.CANVAS_SLOT_COUNT);
    private final ArrayList<Slot> splitSlots = new ArrayList<>(ArtistTableMenu.CANVAS_SLOT_COUNT);
    private final Slot combinedSlot;

    private AbstractCanvasAction action;
    private final ContainerLevelAccess access;

    /*
     * Active only in combination mode, large
     * area for combining canvases, saved in the
     * artist table
     */
    private final ArtistTableGridContainer combinationContainer;

    /*
     * Active always, result slot in combination mode
     * and craft slot in split mode
     */
    private final ItemStackHandler combinedHandler = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            ArtistTableMenu.this.combinedSlotChanged(this);
            super.onContentsChanged(slot);
            //super.onContentsChanged(slot);
        }
    };

    /*
     * Active in split mode, temporary holder
     * for parts of combined canvas, will not be
     * saved in the entity and drop when menu is closed
     * in case if combined slot is empty (after split)
     */
    private final ItemStackHandler splitHandler = new ItemStackHandler(ArtistTableMenu.CANVAS_SLOT_COUNT) {
        @Override
        protected void onContentsChanged(int slot) {
            ArtistTableMenu.this.splitSlotChanged(this);
            super.onContentsChanged(slot);
        }

        @Override
        public void setStackInSlot(int slot, @Nonnull ItemStack stack)
        {
            validateSlotIndex(slot);
            this.stacks.set(slot, stack);
            onContentsChanged(slot);
        }
    };

    // Mode
    private final ContainerData containerData;

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
                           ArtistTableGridContainer artistTableContainer, ContainerData containerData,
                           final ContainerLevelAccess access) {
        super(ZetterContainerMenus.ARTIST_TABLE.get(), windowID);

        this.player = invPlayer.player;
        this.level = invPlayer.player.getLevel();

        this.access = access;

        this.combinationContainer = artistTableContainer;
        this.combinationContainer.addListener(this);

        this.containerData = containerData;

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

                final SlotCombinationGrid combinationSlot = new SlotCombinationGrid(this.combinationContainer, slotNumber,  xpos, ypos);

                this.addSlot(combinationSlot);
                this.gridSlots.add(combinationSlot);
            }
        }

        // Add canvas splitting slots
        for (int y = 0; y < CANVAS_ROW_COUNT; y++) {
            for (int x = 0; x < CANVAS_COLUMN_COUNT; x++) {
                int slotNumber = y * CANVAS_COLUMN_COUNT + x;
                int xpos = COMBINATION_SLOTS_SPLIT_X + x * SLOT_X_SPACING;
                int ypos = COMBINATION_SLOTS_SPLIT_Y + y * SLOT_Y_SPACING;

                final SlotSplitGrid splitSlot = new SlotSplitGrid(this.splitHandler, slotNumber,  xpos, ypos);

                this.addSlot(splitSlot);
                this.splitSlots.add(splitSlot);
            }
        }

        final SlotCombined combinedSlot = new SlotCombined(this.combinedHandler, 0, COMBINED_SLOT_X, COMBINED_SLOT_Y);

        this.addSlot(combinedSlot);
        this.combinedSlot = combinedSlot;

        this.addDataSlots(this.containerData);
        this.addSlotListener(this);

        if (this.getMode() == Mode.COMBINE) {
            this.action = new CanvasCombinationAction(this, this.level);
        } else if (this.getMode() == Mode.SPLIT) {
            this.action = new CanvasSplitAction(this, this.level);
        }
    }

    public static ArtistTableMenu createMenuServerSide(int windowID, Inventory playerInventory,
                                                       ArtistTableGridContainer artistTableContainer,
                                                       ContainerData containerData,
                                                       ContainerLevelAccess access) {
        return new ArtistTableMenu(windowID, playerInventory, artistTableContainer, containerData, access);
    }

    public static ArtistTableMenu createMenuClientSide(int windowID, Inventory playerInventory, net.minecraft.network.FriendlyByteBuf networkBuffer) {
        SArtistTableMenuCreatePacket packet = SArtistTableMenuCreatePacket.readPacketData(networkBuffer);
        ArtistTableGridContainer artistTableContainer = new ArtistTableGridContainer();

        ContainerData clientData = new SimpleContainerData(1);
        clientData.set(ArtistTableBlockEntity.DATA_MODE, packet.getMode().getId());

        return new ArtistTableMenu(windowID, playerInventory, artistTableContainer, new SimpleContainerData(1), ContainerLevelAccess.NULL);
    }

    public ItemStackHandler getCombinationContainer() {
        return this.combinationContainer;
    }

    public ItemStackHandler getCombinedHandler() {
        return this.combinedHandler;
    }

    public ItemStackHandler getSplitHandler() {
        return this.splitHandler;
    }


    /**
     * Get mode from ContainerData
     * @return
     */
    public Mode getMode() {
        return Mode.getById((byte) this.containerData.get(ArtistTableBlockEntity.DATA_MODE));
    }

    /**
     * Set mode directly, on server send update
     * to the Entity's ContainerData
     * @param mode
     */
    public void setMode(Mode mode) {
        if (!this.canChangeMode()) {
            return;
        }

        // @todo: [HIGH]: Eject all slots
        this.setData(ArtistTableBlockEntity.DATA_MODE, mode.getId());

        if (this.player.getLevel().isClientSide()) {
            CArtistTableModeChange unloadPacket = new CArtistTableModeChange(this.containerId, mode);
            ZetterNetwork.simpleChannel.sendToServer(unloadPacket);
        }
    }

    public boolean canChangeMode() {
        if (this.getMode() == Mode.COMBINE) {
            return this.isCombinationGridEmpty();
        } else {
            return this.isCombinedSlotEmpty();
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

    public boolean isSplitGridEmpty() {
        for (Slot combinationSlot : this.splitSlots) {
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
    public void containerChanged(ItemStackHandler container, int slot) {
        this.getAction().onChangedCombination(container);
    }

    public void combinedSlotChanged(ItemStackHandler combinedHandler) {
        this.getAction().onChangedCombined(combinedHandler);
    }

    public void splitSlotChanged(ItemStackHandler combinedHandler) {
        this.getAction().onChangedSplit(combinedHandler);
    }

    /**
     * Check that action model corresponds to
     * the current mode in ContainerData,
     * change action model if it's not,
     * return correct action model
     *
     * It's weird but with ContainerData
     * as single source of truth I'm not sure
     * there's another way to do that
     *
     * @return
     */
    public AbstractCanvasAction getAction() {
        return this.action;
    }

    public AbstractCanvasAction.State getActionState() {
        return this.action.state;
    }

    /**
     * If we requested partial canvas
     *
     * @todo: [MED] Check that handled canvas is in this table!
     *
     * @param canvasCode
     * @param canvasData
     * @param timestamp
     * @return
     */
    public boolean handleCanvasSync(String canvasCode, CanvasData canvasData, long timestamp) {
        this.action.handleCanvasSync(canvasCode, canvasData, timestamp);
        return false;
    }

    @Override
    public void slotChanged(AbstractContainerMenu containerMenu, int slotInd, ItemStack stack) {

    }

    @Override
    public void dataChanged(AbstractContainerMenu containerMenu, int dataSlotIndex, int value) {
        if (dataSlotIndex == DATA_MODE) {
            if (this.getMode() == Mode.COMBINE && !(this.action instanceof CanvasCombinationAction)) {
                this.action = new CanvasCombinationAction(this, this.level);
            }

            if (this.getMode() == Mode.SPLIT && !(this.action instanceof CanvasSplitAction)) {
                this.action = new CanvasSplitAction(this, this.level);
            }

            this.discardSplitGrid(this.player);
        }
    }

    public void setData(int pId, int pData) {
        super.setData(pId, pData);
        this.broadcastChanges();
    }

    /*
      Common handlers
     */

    /**
     *
     * @param player
     */
    public void removed(Player player) {
        super.removed(player);
        this.combinationContainer.removeListener(this);

        // In split mode
        if (this.getMode().equals(Mode.SPLIT)) {
            this.access.execute((level, pos) -> {
                this.clearHandler(player, this.combinedHandler);
            });

            this.discardSplitGrid(player);
        }

        // Manually clean combined canvas as it's unmanaged
        if (this.level.isClientSide()) {
            Helper.getWorldCanvasTracker(this.level).unregisterCanvasData(Helper.COMBINED_CANVAS_CODE);
        }
    }

    /**
     * Called to determine if the current slot is valid for the stack merging (double-click) code. The stack passed in is
     * null for the initial slot that was double-clicked.
     *
     * @todo: this
     */
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slotIn) {
        return super.canTakeItemForPickAll(stack, slotIn);
    }

    /**
     * @todo: this
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
        return this.combinationContainer.stillValid(player);
    }

    /**
     * Helper
     */

    protected void discardSplitGrid(Player player) {
        if (!player.isAlive() || player instanceof ServerPlayer && ((ServerPlayer)player).hasDisconnected()) {
            for(int i = 0; i < this.splitHandler.getSlots(); ++i) {
                ItemStack extractedStack = this.splitHandler.extractItem(i, this.splitHandler.getSlotLimit(i), false);

                if (!extractedStack.isEmpty() && !CanvasItem.isEmpty(extractedStack)) {
                    player.drop(extractedStack, false);
                }
            }

        } else {
            for(int i = 0; i < this.splitHandler.getSlots(); ++i) {
                Inventory inventory = player.getInventory();

                if (inventory.player instanceof ServerPlayer) {
                    ItemStack extractedStack = this.splitHandler.extractItem(i, this.splitHandler.getSlotLimit(i), false);

                    if (!extractedStack.isEmpty() && !CanvasItem.isEmpty(extractedStack)) {
                        inventory.placeItemBackInInventory(extractedStack);
                    }
                }
            }
        }
    }

    protected void clearHandler(Player player, ItemStackHandler stackHandler) {
        if (!player.isAlive() || player instanceof ServerPlayer && ((ServerPlayer)player).hasDisconnected()) {
            for(int i = 0; i < stackHandler.getSlots(); ++i) {
                player.drop(stackHandler.extractItem(i, stackHandler.getSlotLimit(i), false), false);
            }
        } else {
            for(int i = 0; i < stackHandler.getSlots(); ++i) {
                Inventory inventory = player.getInventory();
                if (inventory.player instanceof ServerPlayer) {
                    inventory.placeItemBackInInventory(stackHandler.extractItem(i, stackHandler.getSlotLimit(i), false));
                }
            }

        }
    }

    /**
     * Slots
     */

    public class SlotCombinationGrid extends SlotItemHandler {
        public SlotCombinationGrid(ItemStackHandler stackHandler, int index, int xPosition, int yPosition) {
            super(stackHandler, index, xPosition, yPosition);
        }

        /**
         * We can only use combination grid in combine mode
         * and if container can accept item
         * @param stack
         * @return
         */
        @Override
        public boolean mayPlace(ItemStack stack) {
            return  ArtistTableMenu.this.getMode().equals(Mode.COMBINE) &&
                    ArtistTableMenu.this.combinationContainer.isItemValid(this.getSlotIndex(), stack);
        }

        @Override
        public boolean isActive() {
            return ArtistTableMenu.this.getMode().equals(Mode.COMBINE);
        }

        @Override
        public void onTake(Player player, ItemStack stack) {
            ArtistTableMenu.this.getAction().onTakeCombination(player, stack);
        }
    }

    public class SlotSplitGrid extends SlotItemHandler {
        public SlotSplitGrid(ItemStackHandler stackHandler, int index, int xPosition, int yPosition) {
            super(stackHandler, index, xPosition, yPosition);
        }

        /**
         * Can only take from split grid
         * @param stack
         * @return
         */
        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean isActive() {
            return ArtistTableMenu.this.getMode().equals(Mode.SPLIT);
        }

        @Override
        public void onTake(Player player, ItemStack stack) {
            ArtistTableMenu.this.getAction().onTakeSplit(player, stack);
        }
    }

    public class SlotCombined extends SlotItemHandler {
        public SlotCombined(ItemStackHandler stackHandler, int index, int xPosition, int yPosition) {
            super(stackHandler, index, xPosition, yPosition);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return ArtistTableMenu.this.getAction().mayPlaceCombined(stack);
        }

        @Override
        public void onTake(Player player, ItemStack stack) {
            ArtistTableMenu.this.getAction().onTakeCombined(player, stack);
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
