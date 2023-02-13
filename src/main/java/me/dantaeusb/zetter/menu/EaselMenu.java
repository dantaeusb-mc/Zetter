package me.dantaeusb.zetter.menu;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.client.gui.easel.CanvasWidget;
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
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Consumer;

/**
 * Easel menu, a place where magic happens
 *
 * Weird that we have scale and offset here even though it's client concepts
 * More weird is canvasScaleFactor variable, which actually differs from
 * actual scale (by times 2) and makes a lot of confusion
 * @todo: [LOW] Rework scale factor to avoid confusion
 */
public class EaselMenu extends Container implements EaselStateListener, ItemStackHandlerListener {
    /*
     * Object references
     */
    private final PlayerEntity player;
    private final EaselContainer container;
    private final EaselState state;

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

    /*
     * Update trackers
     */

    private final List<Consumer<AbstractToolParameters>> toolUpdateListeners = new ArrayList<>();
    private final List<Consumer<Integer>> colorUpdateListeners = new ArrayList<>();

    /*
     * Palette
     */

    public static final int PALETTE_SLOTS = PaletteItem.PALETTE_SIZE;

    private int currentPaletteSlot = 0;

    /*
     *
     * Initializing
     *
     */
    public EaselMenu(int windowID, PlayerInventory invPlayer, EaselContainer easelContainer, EaselState stateHandler) {
        super(ZetterContainerMenus.EASEL.get(), windowID);

        this.player = invPlayer.player;

        this.container = easelContainer;
        this.state = stateHandler;

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
                        if (EaselMenu.this.getCurrentTab() == TabsWidget.Tab.INVENTORY) {
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
                        if (EaselMenu.this.getCurrentTab() == TabsWidget.Tab.INVENTORY) {
                            return super.isActive();
                        }

                        return false;
                    }
                });
            }
        }

        this.state.addListener(this);
        this.container.addListener(this);

        // PlayerContainerEvent are not happening on client
        if (this.player.level.isClientSide()) {
            this.state.addPlayer(player);
        }
    }

    public static EaselMenu createMenuServerSide(int windowID, PlayerInventory playerInventory, EaselContainer easelContainer, EaselState stateHandler) {
        EaselMenu easelMenu = new EaselMenu(windowID, playerInventory, easelContainer, stateHandler);

        return easelMenu;
    }

    public static EaselMenu createMenuClientSide(int windowID, PlayerInventory playerInventory, net.minecraft.network.PacketBuffer networkBuffer) {
        SEaselMenuCreatePacket createPacket = SEaselMenuCreatePacket.readPacketData(networkBuffer);

        EaselEntity easelEntity = (EaselEntity) playerInventory.player.level.getEntity(createPacket.easelEntityId);
        assert easelEntity != null;

        EaselContainer easelContainer = easelEntity.getEaselContainer();
        EaselState stateHandler = easelEntity.getStateHandler();

        // Manually update canvas on client
        easelContainer.handleCanvasChange(createPacket.canvasCode);

        return new EaselMenu(windowID, playerInventory, easelContainer, stateHandler);
    }

    /*
     * Canvas
     */

    public static final int MIN_SCALE = 1;
    public static final int MAX_SCALE = 3;

    private int canvasOffsetX = 0;
    private int canvasOffsetY = 0;
    private int canvasScaleFactor = 3;

    public void resetCanvasPositioning() {
        CanvasData canvasData = this.getCanvasData();

        if (canvasData != null) {
            int wScale = CanvasWidget.SIZE / canvasData.getWidth();
            int hScale = CanvasWidget.SIZE / canvasData.getHeight();

            int scaleFactor = Math.min(hScale, wScale) / 2; // / 2 because scale is scaleFactor * 2
            scaleFactor = Math.min(Math.max(scaleFactor, MIN_SCALE), MAX_SCALE);

            int scaledWidth = canvasData.getWidth() * scaleFactor * 2;
            int scaledHeight = canvasData.getHeight() * scaleFactor * 2;

            int offsetX = (CanvasWidget.SIZE - scaledWidth) / 2;
            int offsetY = (CanvasWidget.SIZE - scaledHeight) / 2;

            this.canvasOffsetX = offsetX;
            this.canvasOffsetY = offsetY;
            this.canvasScaleFactor = scaleFactor;
        } else {
            this.canvasOffsetX = 0;
            this.canvasOffsetY = 0;
            this.canvasScaleFactor = 3;
        }
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
        this.state.useTool(this.player, this.currentTool, posX, posY, this.getCurrentColor(), this.getCurrentToolParameters());
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

    public EaselState getState() {
        return this.state;
    }

    /*
     * Listeners
     */

    /**
     * Container updates - canvas and palette
     * @param container
     * @param slot
     */
    @Override
    public void containerChanged(ItemStackHandler container, int slot) {
        // If palette changed
        if (slot == EaselContainer.PALETTE_SLOT) {
            this.notifyColorUpdateListeners();
        } else if (slot == EaselContainer.CANVAS_SLOT) {
            if (this.player.level.isClientSide()) {
                this.resetCanvasPositioning();
            }
        }
    }

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
    }

    public void removeColorUpdateListener(Consumer<Integer> subscriber) {
        if (!this.colorUpdateListeners.contains(subscriber)) {
            return;
        }

        this.colorUpdateListeners.remove(subscriber);
    }

    public void notifyColorUpdateListeners() {
        for (Consumer<Integer> listener: this.colorUpdateListeners) {
            listener.accept(this.getCurrentColor());
        }
    }

    /*
     * Canvas initialization: wait for packet to
     * be sent and not propagate stack updates
     * until then
     *
     * @todo: [HIGH] Does it work without suppression?
     */

    /*
    public void stateCanvasInitializationStart(EaselState state) {
        this.suppressRemoteUpdates();
    }

    public void stateCanvasInitializationEnd(EaselState state) {
        this.resumeRemoteUpdates();
    }
    */

    public void stateCanvasInitializationStart(EaselState state) {

    }

    public void stateCanvasInitializationEnd(EaselState state) {

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

        final boolean result = this.state.undo();

        return result;
    }

    public boolean redo() {
        if (!this.canRedo) {
            return false;
        }

        final boolean result = this.state.redo();

        return result;
    }

    private void updateCanHistory() {
        this.canUndo = this.state.canUndo();
        this.canRedo = this.state.canRedo();
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

            if (this.getCanvasData() != null) {
                // Make sure we're in bounds if canvas size changed
                this.updateCanvasOffset(this.canvasOffsetX, this.canvasOffsetY);
            }

            return false;
        } else {
            this.state.processWeakSnapshotClient(canvasCode, canvasData, timestamp);
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

        this.notifyColorUpdateListeners();

        if (this.player.level.isClientSide()) {
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

    public int getCanvasOffsetX() {
        return this.canvasOffsetX;
    }

    public int getCanvasOffsetY() {
        return this.canvasOffsetY;
    }

    /**
     * Update Canvas offset - usually with Hand tool
     * Makes sure that new position is within bounds where
     * canvas is visible.
     *
     * @param offsetX
     * @param offsetY
     */
    public void updateCanvasOffset(int offsetX, int offsetY) {
        if (this.getCanvasData() == null) {
            return;
        }

        final int width = this.getCanvasData().getWidth() * this.getCanvasScaleFactor() * 2;
        final int minOffsetX = -width + CanvasWidget.SIZE / 2;
        final int maxOffsetX = CanvasWidget.SIZE / 2;

        final int height = this.getCanvasData().getHeight() * this.getCanvasScaleFactor() * 2;
        final int minOffsetY = -height + CanvasWidget.SIZE / 2;
        final int maxOffsetY = CanvasWidget.SIZE / 2;

        this.canvasOffsetX = Math.max(minOffsetX, Math.min(offsetX, maxOffsetX));
        this.canvasOffsetY = Math.max(minOffsetY, Math.min(offsetY, maxOffsetY));
    }

    public int getCanvasScaleFactor() {
        return this.canvasScaleFactor;
    }

    public boolean canIncreaseCanvasScale() {
        return this.getCanvasData() != null && this.canvasScaleFactor < MAX_SCALE;
    }

    public boolean canDecreaseCanvasScale() {
        return this.getCanvasData() != null && this.canvasScaleFactor > MIN_SCALE;
    }

    public boolean increaseCanvasScale() {
        if (!this.canIncreaseCanvasScale()) {
            return false;
        }

        assert this.getCanvasData() != null;
        this.canvasScaleFactor++;

        this.canvasOffsetX -= this.getCanvasData().getWidth();
        this.canvasOffsetY -= this.getCanvasData().getHeight();

        return true;
    }

    public boolean decreaseCanvasScale() {
        if (!this.canDecreaseCanvasScale()) {
            return false;
        }

        assert this.getCanvasData() != null;

        this.canvasScaleFactor--;

        this.canvasOffsetX += this.getCanvasData().getWidth();
        this.canvasOffsetY += this.getCanvasData().getHeight();

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
    public void removed(@Nonnull PlayerEntity player) {
        super.removed(player);

        this.state.removeListener(this);
        this.container.removeListener(this);

        // PlayerContainerEvent are not happening on client
        if (this.player.level.isClientSide()) {
            this.state.removePlayer(player);
        }
    }

    /**
     *
     * @param playerIn
     * @param sourceSlotIndex
     * @return
     */
    @Override
    public ItemStack quickMoveStack(@Nonnull PlayerEntity playerIn, int sourceSlotIndex)
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
    public boolean stillValid(@Nonnull PlayerEntity player) {
        return this.container.stillValid(player);
    }
}
