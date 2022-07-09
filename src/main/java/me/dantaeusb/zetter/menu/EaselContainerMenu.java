package me.dantaeusb.zetter.menu;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.client.gui.painting.TabsWidget;
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
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Consumer;

public class EaselContainerMenu extends AbstractContainerMenu {
    /*
     * Object references
     */
    private final Player player;

    private final ContainerLevelAccess access;
    private final EaselContainer container;
    private final EaselState stateHandler;

    private static final int HOTBAR_SLOT_COUNT = 9;

    private static final int PLAYER_INVENTORY_ROW_COUNT = 3;
    private static final int PLAYER_INVENTORY_COLUMN_COUNT = 9;

    public static final int PLAYER_INVENTORY_XPOS = 38;
    public static final int PLAYER_INVENTORY_YPOS = 156;

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
     *
     * Initializing
     *
     */
    public EaselContainerMenu(int windowID, Inventory invPlayer, EaselContainer easelContainer, EaselState stateHandler, ContainerLevelAccess access) {
        super(ZetterContainerMenus.PAINTING.get(), windowID);

        if (ZetterContainerMenus.PAINTING == null)
            throw new IllegalStateException("Must initialise containerTypeLockTableContainer before constructing a LockTableContainer!");

        this.player = invPlayer.player;

        this.access = access;

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

        if (!invPlayer.player.getLevel().isClientSide()) {
            ItemStack canvasStack = this.container.getCanvasStack();
            String canvasName = CanvasItem.getCanvasCode(canvasStack);
            this.setCanvas(canvasName);
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
        }
    }

    public static EaselContainerMenu createMenuServerSide(int windowID, Inventory playerInventory, EaselContainer easelContainer, EaselState stateHandler, ContainerLevelAccess access) {
        EaselContainerMenu easelContainerMenu = new EaselContainerMenu(windowID, playerInventory, easelContainer, stateHandler, access);

        return easelContainerMenu;
    }

    public static EaselContainerMenu createMenuClientSide(int windowID, Inventory playerInventory, net.minecraft.network.FriendlyByteBuf networkBuffer) {
        SEaselMenuCreatePacket createPacket = SEaselMenuCreatePacket.readPacketData(networkBuffer);

        EaselEntity easelEntity = (EaselEntity) playerInventory.player.getLevel().getEntity(createPacket.getEaselEntityId());
        assert easelEntity != null;

        // it seems like we have to utilize extraData to pass the canvas data
        // slots seems to be synchronised, but we would prefer to avoid that to prevent every-pixel sync
        EaselContainer easelContainer = easelEntity.getEaselContainer();
        EaselState stateHandler = easelEntity.getStateHandler();

        EaselContainerMenu easelContainerMenu = new EaselContainerMenu(windowID, playerInventory, easelContainer, stateHandler, ContainerLevelAccess.NULL);
        easelContainerMenu.setCanvas(createPacket.getCanvasCode());

        return easelContainerMenu;
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

    public void useTool(float posX, float posY) {
        this.stateHandler.useTool(this.player.getUUID(), this.currentTool, posX, posY, this.getCurrentColor(), this.getCurrentToolParameters());
        this.updateCanHistory();
    }

    public AbstractToolParameters getCurrentToolParameters() {
        return ClientPaintingToolParameters.getInstance().getToolParameters(this.currentTool);
    }

    public int getCurrentColor() {
        return this.getPaletteColor(this.getCurrentPaletteSlot());
    }



    /*
     * Listeners
     */

    public void addToolUpdateListener(Consumer<AbstractToolParameters> subscriber) {
        this.toolUpdateListeners.add(subscriber);
        subscriber.accept(this.getCurrentToolParameters());
    }

    public void removeToolUpdateListener(Consumer<AbstractToolParameters> subscriber) {
        this.toolUpdateListeners.remove(subscriber);
    }

    public void addColorUpdateListener(Consumer<Integer> subscriber) {
        this.colorUpdateListeners.add(subscriber);
        subscriber.accept(this.getCurrentColor());
    }

    public void removeColorUpdateListener(Consumer<Integer> subscriber) {
        this.colorUpdateListeners.remove(subscriber);
    }

    /*
     * History
     */

    public boolean canUndo() {
        return this.canUndo;
    }

    public boolean canRedo() {
        return this.canRedo;
    }

    public boolean undo() {
        final boolean result = this.stateHandler.undo(this.player.getUUID());
        this.updateCanHistory();

        return result;
    }

    public boolean redo() {
        final boolean result = this.stateHandler.redo(this.player.getUUID());
        this.updateCanHistory();

        return result;
    }

    private void updateCanHistory() {
        this.canUndo = this.stateHandler.canUndo(this.player.getUUID());
        this.canRedo = this.stateHandler.canRedo(this.player.getUUID());
    }

    /*
     * Networking
     */

    public void handleCanvasSync(String canvasCode, CanvasData canvasData, long packetTimestamp) {
        this.stateHandler.processSyncClient(canvasCode, canvasData, packetTimestamp);
    }

    public void handleCanvasChange(ItemStack canvasStack) {
        /*if (canvasStack.getItem() == ZetterItems.CANVAS.get()) {
            this.canvas = new CanvasHolder(CanvasItem.getCanvasCode(canvasStack), CanvasItem.getCanvasData(canvasStack, this.player.getLevel()));
        } else {
            this.canvas = null;
        }*/
    }

    public void setCanvas(String canvasName) {
        this.container.handleCanvasChange(canvasName);
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
     * Common handlers
     *
     */

    /**
     * Called when the container is closed.
     * Push painting frames so it will be saved
     */
    public void removed(@NotNull Player playerIn) {
        super.removed(playerIn);

        if (this.player.getLevel().isClientSide()) {
            this.stateHandler.poolActionsQueueClient(true);
        }
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
