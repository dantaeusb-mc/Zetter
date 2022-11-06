package me.dantaeusb.zetter.menu;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.client.gui.easel.TabsWidget;
import me.dantaeusb.zetter.client.painting.ClientPaintingToolParameters;
import me.dantaeusb.zetter.core.*;
import me.dantaeusb.zetter.entity.item.EaselEntity;
import me.dantaeusb.zetter.entity.item.state.EaselState;
import me.dantaeusb.zetter.item.CanvasItem;
import me.dantaeusb.zetter.item.PaletteItem;
import me.dantaeusb.zetter.network.packet.CPaletteUpdatePacket;
import me.dantaeusb.zetter.network.packet.SEaselMenuCreatePacket;
import me.dantaeusb.zetter.painting.Tools;
import me.dantaeusb.zetter.painting.parameters.*;
import me.dantaeusb.zetter.entity.item.container.EaselContainer;
import me.dantaeusb.zetter.storage.CanvasData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Consumer;
public class EaselContainerMenu extends AbstractContainerMenu implements EaselStateListener {
    /*
     * Object references
     */
    private final Player player;
    private final EaselContainer container;
    private final EaselState stateHandler;

    private static final int HOTBAR_SLOT_COUNT = 9;

    private static final int PLAYER_INVENTORY_ROW_COUNT = 3;
    private static final int PLAYER_INVENTORY_COLUMN_COUNT = 9;

    public static final int PLAYER_INVENTORY_XPOS = 38;
    public static final int PLAYER_INVENTORY_YPOS = 156;

    public static final int TOTAL_SLOT_COUNT = EaselContainer.STORAGE_SIZE + HOTBAR_SLOT_COUNT + PLAYER_INVENTORY_COLUMN_COUNT * PLAYER_INVENTORY_ROW_COUNT;

    /*
     * Tabs
     */
    private TabsWidget.Tab currentTab = TabsWidget.Tab.COLOR;

    /*
     * Tools
     */
    private Tools currentTool = Tools.PENCIL;

    // Cached values
    private boolean canUndo = false;
    private boolean canRedo = false;

    private final List<Consumer<AbstractToolParameters>> toolUpdateListeners = new ArrayList<>();

    private final List<Consumer<Integer>> colorUpdateListeners = new ArrayList<>();

    /*
     * Palette
     */

    public static final int PALETTE_SLOTS = PaletteItem.PALETTE_SIZE;

    private int currentPaletteSlot = 0;

    /*
     * Client settings
     */

    private int canvasOffsetX = 0;
    private int canvasOffsetY = 0;
    private int canvasScale = 3;

    /*
     *
     * Initializing
     *
     */
    public EaselContainerMenu(int windowID, Inventory invPlayer, EaselContainer easelContainer, EaselState stateHandler) {
        super(ZetterContainerMenus.EASEL.get(), windowID);

        if (ZetterContainerMenus.EASEL == null)
            throw new IllegalStateException("Must initialise containerTypeLockTableContainer before constructing a LockTableContainer!");

        this.player = invPlayer.player;

        this.container = easelContainer;
        this.stateHandler = stateHandler;

        final int CANVAS_SLOT_X = 180;
        final int CANVAS_SLOT_Y = 9;

        final int PALETTE_SLOT_X = 180;
        final int PALETTE_SLOT_Y = 132;

        final int SLOT_X_SPACING = 18;
        final int SLOT_Y_SPACING = 18;
        final int HOTBAR_XPOS = 38;
        final int HOTBAR_YPOS = 214;

        this.addSlot(new SlotItemHandler(this.container, EaselContainer.CANVAS_SLOT, CANVAS_SLOT_X, CANVAS_SLOT_Y) {
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() == ZetterItems.CANVAS.get();
            }
        });

        this.addSlot(new SlotItemHandler(this.container, EaselContainer.PALETTE_SLOT, PALETTE_SLOT_X, PALETTE_SLOT_Y) {
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() == ZetterItems.PALETTE.get();
            }
        });

        // Add the rest of the players inventory to the gui
        for (int y = 0; y < PLAYER_INVENTORY_ROW_COUNT; y++) {
            for (int x = 0; x < PLAYER_INVENTORY_COLUMN_COUNT; x++) {
                int slotNumber = HOTBAR_SLOT_COUNT + y * PLAYER_INVENTORY_COLUMN_COUNT + x;
                int xpos = PLAYER_INVENTORY_XPOS + x * SLOT_X_SPACING;
                int ypos = PLAYER_INVENTORY_YPOS + y * SLOT_Y_SPACING;

                this.addSlot(new Slot(invPlayer, slotNumber,  xpos, ypos) {
                    @Override
                    public boolean isActive() {
                        if (EaselContainerMenu.this.getCurrentTab() == TabsWidget.Tab.INVENTORY) {
                            return super.isActive();
                        }

                        return false;
                    }
                });
            }

            // Add the players hotbar to the gui - the [xpos, ypos] location of each item
            for (int x = 0; x < HOTBAR_SLOT_COUNT; x++) {
                int slotNumber = x;
                addSlot(new Slot(invPlayer, slotNumber, HOTBAR_XPOS + SLOT_X_SPACING * x, HOTBAR_YPOS) {
                    @Override
                    public boolean isActive() {
                        if (EaselContainerMenu.this.getCurrentTab() == TabsWidget.Tab.INVENTORY) {
                            return super.isActive();
                        }

                        return false;
                    }
                });
            }
        }

        this.stateHandler.addListener(this);
    }

    public static EaselContainerMenu createMenuServerSide(int windowID, Inventory playerInventory, EaselContainer easelContainer, EaselState stateHandler) {
        EaselContainerMenu easelContainerMenu = new EaselContainerMenu(windowID, playerInventory, easelContainer, stateHandler);

        return easelContainerMenu;
    }

    public static EaselContainerMenu createMenuClientSide(int windowID, Inventory playerInventory, net.minecraft.network.FriendlyByteBuf networkBuffer) {
        SEaselMenuCreatePacket createPacket = SEaselMenuCreatePacket.readPacketData(networkBuffer);

        EaselEntity easelEntity = (EaselEntity) playerInventory.player.getLevel().getEntity(createPacket.easelEntityId);
        assert easelEntity != null;

        EaselContainer easelContainer = easelEntity.getEaselContainer();
        EaselState stateHandler = easelEntity.getStateHandler();

        // Manually update canvas on client @todo: [HIGH] Create snapshot
        easelContainer.handleCanvasChange(createPacket.canvasCode);
        //stateHandler.processSnapshotSyncClient();

        return new EaselContainerMenu(windowID, playerInventory, easelContainer, stateHandler);
    }

    /*
     *
     * Getter-setters
     *
     */

    public int getCurrentPaletteSlot() {
        return this.currentPaletteSlot;
    }

    public void setCurrentPaletteSlot(int slotIndex) {
        this.currentPaletteSlot = slotIndex;

        for (Consumer<Integer> listener: this.colorUpdateListeners) {
            listener.accept(this.getCurrentColor());
        }
    }

    public Tools getCurrentTool() {
        return this.currentTool;
    }

    public void setCurrentTool(Tools tool) {
        this.currentTool = tool;

        for (Consumer<AbstractToolParameters> listener: this.toolUpdateListeners) {
            listener.accept(this.getCurrentToolParameters());
        }
    }

    /*
     * Tool
     */
    public void useTool(float posX, float posY) {
        this.stateHandler.useTool(this.player.getUUID(), this.currentTool, posX, posY, this.getCurrentColor(), this.getCurrentToolParameters());
    }

    public AbstractToolParameters getCurrentToolParameters() {
        return ClientPaintingToolParameters.getInstance().getToolParameters(this.currentTool);
    }

    public int getCurrentColor() {
        return this.getPaletteColor(this.getCurrentPaletteSlot());
    }

    public EaselContainer getContainer() {
        return this.container;
    }

    /*
     * Listeners
     */

    public void addToolUpdateListener(Consumer<AbstractToolParameters> subscriber) {
        if (this.toolUpdateListeners.contains(subscriber)) {
            return;
        }

        this.toolUpdateListeners.add(subscriber);
        subscriber.accept(this.getCurrentToolParameters());
    }

    public void removeToolUpdateListener(Consumer<AbstractToolParameters> subscriber) {
        if (!this.toolUpdateListeners.contains(subscriber)) {
            return;
        }

        this.toolUpdateListeners.remove(subscriber);
    }

    public void addColorUpdateListener(Consumer<Integer> subscriber) {
        if (this.colorUpdateListeners.contains(subscriber)) {
            return;
        }

        this.colorUpdateListeners.add(subscriber);
        subscriber.accept(this.getCurrentColor());
    }

    public void removeColorUpdateListener(Consumer<Integer> subscriber) {
        if (!this.colorUpdateListeners.contains(subscriber)) {
            return;
        }

        this.colorUpdateListeners.remove(subscriber);
    }

    /*
     * History
     */

    @Override
    public void stateChanged(EaselState state) {
        this.updateCanHistory();
    }

    public boolean canUndo() {
        return this.canUndo;
    }

    public boolean canRedo() {
        return this.canRedo;
    }

    public boolean undo() {
        if (!this.canUndo) {
            return false;
        }

        final boolean result = this.stateHandler.undo(this.player.getUUID());

        return result;
    }

    public boolean redo() {
        if (!this.canRedo) {
            return false;
        }

        final boolean result = this.stateHandler.redo(this.player.getUUID());

        return result;
    }

    private void updateCanHistory() {
        this.canUndo = this.stateHandler.canUndo(this.player.getUUID());
        this.canRedo = this.stateHandler.canRedo(this.player.getUUID());
    }

    /*
     * Networking
     */

    /**
     * Returns true if event is consumed and canvas data don't have to be registered with canvasTracker.registerCanvasData
     *
     * @param canvasCode
     * @param canvasData
     * @param timestamp
     * @return consumed
     */
    public boolean handleCanvasSync(String canvasCode, CanvasData canvasData, long timestamp) {
        // Only update if we don't have canvas initialized or name changed
        if (this.container.getCanvas() == null || !this.container.getCanvas().code.equals(canvasCode)) {
            this.container.handleCanvasChange(canvasCode);
            return false;
        } else {
            this.stateHandler.processWeakSnapshotClient(canvasCode, canvasData, timestamp);
            return true;
        }
    }

    public int getPaletteColor(int paletteSlot) {
        ItemStack paletteStack = this.container.getPaletteStack();

        if (paletteStack.isEmpty()) {
            return 0xFF000000;
        }

        return PaletteItem.getPaletteColors(paletteStack)[paletteSlot];
    }

    public void setCurrentTab(TabsWidget.Tab tab) {
        this.currentTab = tab;
    }

    public TabsWidget.Tab getCurrentTab() {
        return this.currentTab;
    }

    /**
     * This won't update palette color on the other side and used for quick adjustment
     * @param color
     */
    public void setPaletteColor(int color) {
        this.setPaletteColor(color, this.currentPaletteSlot);
    }

    public void setPaletteColor(int color, int slot) {
        ItemStack paletteStack = this.container.getPaletteStack();

        if (paletteStack.isEmpty()) {
            return;
        }

        PaletteItem.updatePaletteColor(paletteStack, slot, color);

        for (Consumer<Integer> listener: this.colorUpdateListeners) {
            listener.accept(this.getCurrentColor());
        }

        if (this.player.getLevel().isClientSide()) {
            this.sendPaletteUpdatePacket();
        }

        this.container.changed();
    }

    public void sendPaletteUpdatePacket() {
        if (!this.isPaletteAvailable()) {
            return;
        }

        CPaletteUpdatePacket paletteUpdatePacket = new CPaletteUpdatePacket(this.currentPaletteSlot, this.getCurrentColor());
        Zetter.LOG.debug("Sending Palette Update: " + paletteUpdatePacket);
        ZetterNetwork.simpleChannel.sendToServer(paletteUpdatePacket);
    }

    /**
     * Alternative check using item instead of holder,
     * used for cases when holder is not initialized (canvas not loaded)
     * @return
     */
    public @Nullable String getCanvasItemCode() {
        ItemStack canvasStack = this.container.getCanvasStack();

        if (canvasStack.isEmpty()) {
            return null;
        }

        return CanvasItem.getCanvasCode(canvasStack);
    }

    public @Nullable String getCanvasCode() {
        if (this.container.getCanvas() == null) {
            return null;
        }

        return this.container.getCanvas().code;
    }

    public @Nullable CanvasData getCanvasData() {
        if (this.container.getCanvas() == null) {
            return null;
        }

        return this.container.getCanvas().data;
    }

    public boolean isCanvasAvailable() {
        return this.container.getCanvas() != null;
    }

    public boolean isPaletteAvailable() {
        ItemStack paletteStack = this.container.getPaletteStack();

        return !paletteStack.isEmpty();
    }

    /*
     *
     * Client things
     *
     */

    public int getCanvasScale() {
        return this.canvasScale;
    }

    public boolean canIncreaseCanvasScale() {
        return this.canvasScale < 3;
    }

    public boolean canDecreaseCanvasScale() {
        return this.canvasScale > 1;
    }

    public boolean increaseCanvasScale() {
        if (!this.canIncreaseCanvasScale()) {
            return false;
        }

        this.canvasScale++;

        return true;
    }

    public boolean decreaseCanvasScale() {
        if (!this.canDecreaseCanvasScale()) {
            return false;
        }

        this.canvasScale--;

        return true;
    }

    /*
     *
     * Common handlers
     *
     */

    /**
     * Called when the ONLY when container is closed.
     * Push painting frames so it will be saved.
     */
    public void removed(@NotNull Player playerIn) {
        super.removed(playerIn);

        if (this.player.getLevel().isClientSide()) {
            this.stateHandler.poolActionsQueueClient(true);
        }

        this.stateHandler.removeListener(this);
    }

    /**
     *
     * @param playerIn
     * @param sourceSlotIndex
     * @return
     */
    @Override
    public ItemStack quickMoveStack(@NotNull Player playerIn, int sourceSlotIndex)
    {
        ItemStack outStack = ItemStack.EMPTY;
        Slot sourceSlot = this.slots.get(sourceSlotIndex);

        if (sourceSlot != null && sourceSlot.hasItem()) {
            ItemStack sourceStack = sourceSlot.getItem();
            outStack = sourceStack.copy();

            // Canvas or Palette
            if (sourceSlotIndex <= 1) {
                if (!this.moveItemStackTo(sourceStack, 2, TOTAL_SLOT_COUNT, true)) {
                    return ItemStack.EMPTY;
                }
            // Inventory
            } else {
                if (sourceStack.getItem() == ZetterItems.PALETTE.get()) {
                    if (!this.moveItemStackTo(sourceStack, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (sourceStack.getItem() == ZetterItems.CANVAS.get()) {
                    if (!this.moveItemStackTo(sourceStack, 1, 2, false)) {
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
    public boolean stillValid(@NotNull Player player) {
        return this.container.stillValid(player);
    }
}
