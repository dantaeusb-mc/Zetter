package me.dantaeusb.zetter.menu;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.canvastracker.CanvasServerTracker;
import me.dantaeusb.zetter.canvastracker.ICanvasTracker;
import me.dantaeusb.zetter.client.gui.painting.TabsWidget;
import me.dantaeusb.zetter.core.*;
import me.dantaeusb.zetter.entity.item.EaselEntity;
import me.dantaeusb.zetter.menu.painting.PaintingActionBuffer;
import me.dantaeusb.zetter.item.CanvasItem;
import me.dantaeusb.zetter.item.PaletteItem;
import me.dantaeusb.zetter.menu.painting.parameters.*;
import me.dantaeusb.zetter.menu.painting.tools.*;
import me.dantaeusb.zetter.network.packet.CCanvasActionBufferPacket;
import me.dantaeusb.zetter.network.packet.CPaletteUpdatePacket;
import me.dantaeusb.zetter.network.packet.SCanvasNamePacket;
import me.dantaeusb.zetter.storage.util.CanvasHolder;
import me.dantaeusb.zetter.entity.item.container.EaselContainer;
import me.dantaeusb.zetter.storage.CanvasData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.SlotItemHandler;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Consumer;

public class EaselContainerMenu extends AbstractContainerMenu {
    /*
     * Object references
     */

    private final Player player;
    private final Level world;
    private EaselEntity entity;
    private final EaselContainer easelContainer;

    private static final int HOTBAR_SLOT_COUNT = 9;

    private static final int PLAYER_INVENTORY_ROW_COUNT = 3;
    private static final int PLAYER_INVENTORY_COLUMN_COUNT = 9;

    public static final int PLAYER_INVENTORY_XPOS = 38;
    public static final int PLAYER_INVENTORY_YPOS = 156;

    private int tick = 0;

    /*
     * Canvas
     */
    private @Nullable CanvasHolder<CanvasData> canvas;

    /*
     * State and networking
     */

    // @todo: actually move to container to share between players?
    // Only player's buffer on client, all users on server
    private final Deque<PaintingActionBuffer> actionsQueue = new LinkedList<>();

    // Saved painting states
    private Queue<int[]> snapshots;

    /*
     * Tabs
     */

    private TabsWidget.Tab currentTab = TabsWidget.Tab.COLOR;

    /*
     * Tools
     */

    // Client-only I think
    private HashMap<String, AbstractToolParameters> toolParameters = new HashMap<>(){{
        put(Pencil.CODE, new PencilParameters());
        put(Brush.CODE, new BrushParameters());
    }};

    private String currentTool = Pencil.CODE;

    // Tools available for use
    private final HashMap<String, AbstractTool> tools = new HashMap<>(){{
        put(Pencil.CODE, new Pencil(EaselContainerMenu.this));
        put(Brush.CODE, new Brush(EaselContainerMenu.this));
        put(Bucket.CODE, new Bucket(EaselContainerMenu.this));
        put(Eyedropper.CODE, new Eyedropper(EaselContainerMenu.this));
    }};

    private List<Consumer<AbstractToolParameters>> toolUpdateListeners = new ArrayList<>();

    private List<Consumer<Integer>> colorUpdateListeners = new ArrayList<>();

    /*
     * Palette
     */

    public static final int PALETTE_SLOTS = 14;

    private int currentPaletteSlot = 0;

    /*
     *
     * Initializing
     *
     */
    public EaselContainerMenu(int windowID, Inventory invPlayer, EaselContainer easelContainer) {
        super(ZetterContainerMenus.PAINTING.get(), windowID);

        if (ZetterContainerMenus.PAINTING == null)
            throw new IllegalStateException("Must initialise containerTypeLockTableContainer before constructing a LockTableContainer!");

        this.player = invPlayer.player;
        this.world = invPlayer.player.level;
        this.easelContainer = easelContainer;

        final int CANVAS_SLOT_X = 180;
        final int CANVAS_SLOT_Y = 9;

        final int PALETTE_SLOT_X = 180;
        final int PALETTE_SLOT_Y = 132;

        final int SLOT_X_SPACING = 18;
        final int SLOT_Y_SPACING = 18;
        final int HOTBAR_XPOS = 38;
        final int HOTBAR_YPOS = 214;

        this.addSlot(new SlotItemHandler(this.easelContainer, EaselContainer.CANVAS_SLOT, CANVAS_SLOT_X, CANVAS_SLOT_Y) {
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() == ZetterItems.CANVAS.get();
            }
        });

        this.addSlot(new SlotItemHandler(this.easelContainer, EaselContainer.PALETTE_SLOT, PALETTE_SLOT_X, PALETTE_SLOT_Y) {
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() == ZetterItems.PALETTE.get();
            }
        });

        if (!this.world.isClientSide()) {
            ItemStack canvasStack = this.easelContainer.getCanvasStack();
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

    public static EaselContainerMenu createMenuServerSide(int windowID, Inventory playerInventory, EaselEntity entity, EaselContainer easelContainer) {
        EaselContainerMenu easelContainerMenu = new EaselContainerMenu(windowID, playerInventory, easelContainer);
        easelContainerMenu.setEntity(entity);

        return easelContainerMenu;
    }

    public static EaselContainerMenu createMenuClientSide(int windowID, Inventory playerInventory, net.minecraft.network.FriendlyByteBuf networkBuffer) {
        // it seems like we have to utilize extraData to pass the canvas data
        // slots seems to be synchronised, but we would prefer to avoid that to prevent every-pixel sync
        EaselContainer easelContainer = new EaselContainer();

        String canvasName = SCanvasNamePacket.readCanvasName(networkBuffer);

        EaselContainerMenu easelContainerMenu = new EaselContainerMenu(windowID, playerInventory, easelContainer);
        easelContainerMenu.setCanvas(canvasName);

        return easelContainerMenu;
    }

    /*
     *
     * Getter-setters
     *
     */
    @Nullable
    public EaselEntity getEntity() {
        return this.entity;
    }

    public void setEntity(EaselEntity entity)
    {
        this.entity = entity;
    }
    public int getCurrentPaletteSlot() {
        return this.currentPaletteSlot;
    }

    public void setCurrentPaletteSlot(int slotIndex) {
        this.currentPaletteSlot = slotIndex;

        for (Consumer<Integer> listener: this.colorUpdateListeners) {
            listener.accept(this.getCurrentColor());
        }
    }

    public HashMap<String, AbstractTool> getTools() {
        return this.tools;
    }

    public AbstractTool getCurrentTool() {
        return this.getTool(this.currentTool);
    }

    public AbstractTool getTool(String toolCode) {
        return this.tools.get(toolCode);
    }

    public void setCurrentTool(String toolCode) {
        if (!this.tools.containsKey(toolCode)) {
            throw new IllegalStateException("No such tool: " + toolCode);
        }

        this.currentTool = toolCode;

        for (Consumer<AbstractToolParameters> listener: this.toolUpdateListeners) {
            listener.accept(this.getCurrentToolParameters());
        }
    }

    public AbstractToolParameters getCurrentToolParameters() {
        return this.toolParameters.get(this.currentTool);
    }

    public int getCurrentColor() {
        return this.getPaletteColor(this.getCurrentPaletteSlot());
    }

    /*
     *
     * Application logic
     *
     */

    /**
     * Apply current tool at certain position and record action if sucessful
     * @param posX
     * @param posY
     */
    public void useTool(float posX, float posY) {
        // No palette or no paints left
        if (this.easelContainer.getPaletteStack().isEmpty() || this.easelContainer.getPaletteStack().getDamageValue() >= this.easelContainer.getPaletteStack().getMaxDamage() - 1) {
            return;
        }

        if (this.getCanvasData() == null) {
            return;
        }

        if (!this.checkActionSafety(posX, posY)) {
            Zetter.LOG.warn("Unsafe action: X:" + posX + " Y:" + posY);
            return;
        }

        PaintingActionBuffer lastAction = null;

        if (!this.actionsQueue.isEmpty()) {
             lastAction = this.actionsQueue.getLast();
        }

        Float lastX = null, lastY = null;

        // @todo: when we will do tick/commit, it will be resetting after .5s allowing to do dot dot dot?
        if (lastAction != null && lastAction.toolCode.equals(this.currentTool) && !lastAction.isCommitted()) {
            final PaintingActionBuffer.PaintingAction lastSubAction = lastAction.getLastAction();

            if (lastSubAction != null) {
                lastX = lastSubAction.posX;
                lastY = lastSubAction.posY;
            }
        }

        if (this.getCurrentTool().shouldAddAction(posX, posY, lastX, lastY)) {
            int damage = this.getCurrentTool().apply(this.getCanvasData(), this.getCurrentToolParameters(), this.getCurrentColor(), posX, posY);
            this.recordAction(posX, posY);

            this.easelContainer.damagePalette(damage);
        }
    }

    private boolean checkActionSafety(float posX, float posY) {
        if (posX < 0 || posY < 0) {
            return false;
        }

        if (posX > this.getCanvasData().getWidth() || posY > this.getCanvasData().getHeight()) {
            return false;
        }

        return true;
    }

    private void recordAction(float posX, float posY) {
        PaintingActionBuffer lastAction = this.actionsQueue.peekLast();

        if (lastAction == null) {
            lastAction = this.createAction();
        } else if (!lastAction.canContinue(this.player.getUUID(), this.currentTool, this.getCurrentToolParameters())) {
            lastAction.commit();
            lastAction = this.createAction();
        }

        lastAction.addFrame(posX, posY);
    }

    private PaintingActionBuffer createAction() {
        final PaintingActionBuffer lastAction = this.actionsQueue.peekLast();

        if (lastAction != null && !lastAction.isCommitted()) {
            lastAction.commit();
        }

        final PaintingActionBuffer newAction = new PaintingActionBuffer(this.player.getUUID(), this.currentTool, this.getCurrentToolParameters());
        this.actionsQueue.add(newAction);

        return newAction;
    }

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

    public void handleCanvasChange(ItemStack canvasStack) {
        if (canvasStack.getItem() == ZetterItems.CANVAS.get()) {
            this.canvas = new CanvasHolder(CanvasItem.getCanvasCode(canvasStack), CanvasItem.getCanvasData(canvasStack, this.world));
        } else {
            this.canvas = null;
        }
    }

    public void setCanvas(String canvasName) {
        if (canvasName == null || canvasName.equals(CanvasData.getCanvasCode(0))) {
            this.canvas = null;
            return;
        }

        ICanvasTracker canvasTracker;

        if (this.world.isClientSide()) {
            canvasTracker = this.world.getCapability(ZetterCapabilities.CANVAS_TRACKER).orElse(null);
        } else {
            canvasTracker = this.world.getServer().overworld().getCapability(ZetterCapabilities.CANVAS_TRACKER).orElse(null);
        }

        if (canvasTracker == null) {
            Zetter.LOG.error("Cannot find world canvas capability");
            this.canvas = null;
            return;
        }

        CanvasData canvas = canvasTracker.getCanvasData(canvasName, CanvasData.class);

        if (canvas == null) {
            this.canvas = null;
            return;
        }

        this.canvas = new CanvasHolder<>(canvasName, canvas);
    }

    public int getPaletteColor(int paletteSlot) {
        ItemStack paletteStack = this.easelContainer.getPaletteStack();

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
        ItemStack paletteStack = this.easelContainer.getPaletteStack();

        if (paletteStack.isEmpty()) {
            return;
        }

        PaletteItem.updatePaletteColor(paletteStack, slot, color);

        if (this.world.isClientSide) {
            this.sendPaletteUpdatePacket();
        }

        this.easelContainer.changed();
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
        if (this.canvas == null) {
            return null;
        }

        return this.canvas.code;
    }

    public @Nullable CanvasData getCanvasData() {
        if (this.canvas == null) {
            return null;
        }

        return this.canvas.data;
    }

    public boolean isCanvasAvailable() {
        return this.canvas != null;
    }

    public boolean isPaletteAvailable() {
        ItemStack paletteStack = this.easelContainer.getPaletteStack();

        return !paletteStack.isEmpty();
    }

    public void tick() {
        this.tickActionsQueue();
    }

    /*
     *
     * Networking
     *
     */

    /**
     * Checks and sends action buffer on client
     */
    protected void tickActionsQueue() {
        if (++this.tick % 20 != 0) {
            return;
        }

        final Queue<PaintingActionBuffer> unsentActions = new LinkedList<>();
        Iterator<PaintingActionBuffer> iterator = this.actionsQueue.descendingIterator();

        while(iterator.hasNext()) {
            PaintingActionBuffer paintingActionBuffer = iterator.next();

            if (!paintingActionBuffer.isCommitted()) {
                if (paintingActionBuffer.shouldCommit()) {
                    paintingActionBuffer.commit();
                } else {
                    continue;
                }
            }

            if (paintingActionBuffer.isSent()) {
                break;
            }

            unsentActions.add(paintingActionBuffer);
        }

        if (!unsentActions.isEmpty()) {
            CCanvasActionBufferPacket paintingFrameBufferPacket = new CCanvasActionBufferPacket(unsentActions);
            ZetterNetwork.simpleChannel.sendToServer(paintingFrameBufferPacket);

            for (PaintingActionBuffer unsentAction : unsentActions) {
                unsentAction.setSent();
            }
        }
    }

    /**
     * Called from network - process player's work (only on server)
     * @param actionBuffer
     */
    public void processFrameBufferServer(PaintingActionBuffer actionBuffer, UUID ownerId) {
        /*long currentTime = this.world.getGameTime();

        for (PaintingActionFrame frame: paintingFrameBuffer.getFrames(ownerId)) {
            if (currentTime - frame.getFrameTime() > FRAME_TIMEOUT) {
                // Update will be sent to player anyway, and they'll request new sync if they're confused
                Zetter.LOG.debug("Skipping painting frame, too old");
                continue;
            }

            //this.writePixelOnCanvasServerSide(frame.getPixelIndex(), frame.getColor());
        }*/

        ((CanvasServerTracker) Helper.getWorldCanvasTracker(this.world)).markCanvasDesync(this.canvas.code);
    }

    /**
     * Add all newest pixels to the canvas when syncing to keep recent player's changesv
     * @param canvasCode
     * @param canvasData
     * @param packetTimestamp
     */
    public void processSync(String canvasCode, CanvasData canvasData, long packetTimestamp) {
        /*long timestamp = System.currentTimeMillis();

        PlayerInfo playerInfo = Minecraft.getInstance().getConnection().getPlayerInfo(this.player.getUUID());
        int latency = playerInfo.getLatency();

        latency *= 1.1; // 10% jitter
        latency = Math.max(latency, 50);

        // Ok, so if this value is low - client is pushing pixels, so trusted interval should be shorter
        // If it's 500, we should use full trusted interval
        // Maybe track how many pixels sent or check last frame write
        long timeSinceLastFrameBufferSent = timestamp - this.lastFrameBufferSendClock;

        // Trusted interval should be used for mightDesync
        // Everything out ot earliest bound of trusted interval should be removed from frames list

        long adjustedTimestamp = System.currentTimeMillis();
        adjustedTimestamp -= latency * 2L;
        adjustedTimestamp -= timeSinceLastFrameBufferSent;
        
        boolean mightDesync = false;

        // Throw out pixels drawn before start of the interval, ask for resync for interval, cause not sure if
        // they're sync, just add pixels for newer changes, they'll be pushed anyway
        Tuple<Long, Long> lowTrustInterval = new Tuple<>(adjustedTimestamp, timestamp - this.lastFrameBufferSendClock);

        Zetter.LOG.debug("Latency: " + latency);

        for (PaintingActionFrame oldFrame: this.lastFrames) {
            if (oldFrame.getFrameTime() < lowTrustInterval.getA()) {
                // @todo: reasonable to remove older frames right there
            } else {
                if (oldFrame.getFrameTime() < lowTrustInterval.getB()) {
                    mightDesync = true;
                }

                canvasData.updateCanvasPixel(oldFrame.getPixelIndex(), oldFrame.getColor());
            }
        }

        if (mightDesync) {
            CanvasRenderer.getInstance().queueCanvasTextureUpdate(canvasData.getType(), canvasCode);
        }

        this.canvas = new CanvasHolder<>(canvasCode, canvasData);

        this.lastSyncReceivedClock = System.currentTimeMillis();*/
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
    public void removed(Player playerIn) {
        super.removed(playerIn);

        /*if (this.world.isClientSide() && !this.getCanvasChanges().isEmpty()) {
            this.canvasChanges.getFrames(playerIn.getUUID());
            CPaintingFrameBufferPacket modePacket = new CPaintingFrameBufferPacket(this.canvasChanges);
            ZetterNetwork.simpleChannel.sendToServer(modePacket);

            this.lastFrameBufferSendClock = System.currentTimeMillis();
        }*/
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
        return this.easelContainer.stillValid(player);
    }
}
