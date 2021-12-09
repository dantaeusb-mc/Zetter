package com.dantaeusb.zetter.menu;

import com.dantaeusb.zetter.Zetter;
import com.dantaeusb.zetter.canvastracker.CanvasServerTracker;
import com.dantaeusb.zetter.canvastracker.ICanvasTracker;
import com.dantaeusb.zetter.client.renderer.CanvasRenderer;
import com.dantaeusb.zetter.core.*;
import com.dantaeusb.zetter.entity.item.EaselEntity;
import com.dantaeusb.zetter.menu.painting.PaintingFrame;
import com.dantaeusb.zetter.menu.painting.PaintingFrameBuffer;
import com.dantaeusb.zetter.item.CanvasItem;
import com.dantaeusb.zetter.item.PaletteItem;
import com.dantaeusb.zetter.network.packet.*;
import com.dantaeusb.zetter.storage.util.CanvasHolder;
import com.dantaeusb.zetter.tileentity.container.EaselContainer;
import com.dantaeusb.zetter.storage.CanvasData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.util.Tuple;
import net.minecraft.Util;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fmllegacy.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Stream;

public class EaselContainerMenu extends AbstractContainerMenu {
    public static int FRAME_TIMEOUT = 5000;  // 5 second limit to keep changes buffer, if packet processed later disregard it

    private final Player player;
    private final Level world;
    private EaselEntity entity;

    public static final int PALETTE_SLOTS = 14;
    private static final int HOTBAR_SLOT_COUNT = 9;

    private @Nullable CanvasHolder<CanvasData> canvas;
    private final EaselContainer easelContainer;

    /**
     * Not needed to be map if there's multiple containers for one TE on server
     */
    // Only player's buffer on client, all users on server
    private PaintingFrameBuffer canvasChanges;

    private final LinkedList<PaintingFrame> lastFrames = new LinkedList<>();

    private long lastFrameBufferSendClock = 0L;
    private long lastSyncReceivedClock = 0L;
    private long lastPushedFrameClock = 0L;

    private Notify firstLoadNotification = ()->{};

    public EaselContainerMenu(int windowID, Inventory invPlayer, EaselContainer easelContainer) {
        super(ModContainers.PAINTING, windowID);

        if (ModContainers.PAINTING == null)
            throw new IllegalStateException("Must initialise containerTypeLockTableContainer before constructing a LockTableContainer!");

        this.player = invPlayer.player;
        this.world = invPlayer.player.level;
        this.easelContainer = easelContainer;
        this.canvasChanges = new PaintingFrameBuffer(System.currentTimeMillis());

        final int PALETTE_SLOT_X_SPACING = 152;
        final int PALETTE_SLOT_Y_SPACING = 94;

        final int HOTBAR_XPOS = 8;
        final int HOTBAR_YPOS = 161;
        final int SLOT_X_SPACING = 18;

        this.addSlot(new Slot(this.easelContainer, EaselContainer.PALETTE_SLOT, PALETTE_SLOT_X_SPACING, PALETTE_SLOT_Y_SPACING) {
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() == ModItems.PALETTE;
            }
        });

        if (!this.world.isClientSide()) {
            ItemStack canvasStack = this.easelContainer.getItem(EaselContainer.CANVAS_SLOT);
            String canvasName = CanvasItem.getCanvasCode(canvasStack);
            this.setCanvas(canvasName);
        }

        // Add the players hotbar to the gui - the [xpos, ypos] location of each item
        for (int x = 0; x < HOTBAR_SLOT_COUNT; x++) {
            int slotNumber = x;
            addSlot(new Slot(invPlayer, slotNumber, HOTBAR_XPOS + SLOT_X_SPACING * x, HOTBAR_YPOS));
        }
    }

    public void setEntity(EaselEntity entity)
    {
        this.entity = entity;
    }

    public static EaselContainerMenu createContainerServerSide(int windowID, Inventory playerInventory, EaselEntity entity, EaselContainer easelContainer) {
        EaselContainerMenu easelContainerMenu = new EaselContainerMenu(windowID, playerInventory, easelContainer);
        easelContainerMenu.setEntity(entity);

        return easelContainerMenu;
    }

    public static EaselContainerMenu createContainerClientSide(int windowID, Inventory playerInventory, net.minecraft.network.FriendlyByteBuf networkBuffer) {
        // it seems like we have to utilize extraData to pass the canvas data
        // slots seems to be synchronised, but we would prefer to avoid that to prevent every-pixel sync
        EaselContainer easelContainer = new EaselContainer();

        String canvasName = SCanvasNamePacket.readCanvasName(networkBuffer);

        EaselContainerMenu easelContainerMenu = new EaselContainerMenu(windowID, playerInventory, easelContainer);
        easelContainerMenu.setCanvas(canvasName);

        return easelContainerMenu;
    }

    @Nullable
    public EaselEntity getEntity() {
        return this.entity;
    }

    public EaselContainer getEaselContainer() {
        return this.easelContainer;
    }

    /**
     * Because we don't have a special Slot for the canvas, we're using custom
     * update functions and network message
     */

    public void setFirstLoadNotification(Notify firstLoadNotification) {
        this.firstLoadNotification = firstLoadNotification;
    }

    public void handleCanvasChange(ItemStack canvasStack) {
        if (canvasStack.getItem() == ModItems.CANVAS) {
            this.canvas = new CanvasHolder(CanvasItem.getCanvasCode(canvasStack), CanvasItem.getCanvasData(canvasStack, this.world));
        } else {
            this.canvas = null;
        }

        if (this.firstLoadNotification != null) {
            this.firstLoadNotification.invoke();
        }
    }

    public void setCanvas(String canvasName) {
        if (canvasName == null || canvasName.equals(CanvasData.getCanvasCode(0))) {
            this.canvas = null;
            return;
        }

        ICanvasTracker canvasTracker;

        if (this.world.isClientSide()) {
            canvasTracker = this.world.getCapability(ModCapabilities.CANVAS_TRACKER).orElse(null);
        } else {
            canvasTracker = this.world.getServer().overworld().getCapability(ModCapabilities.CANVAS_TRACKER).orElse(null);
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

    /**
     * This won't update palette color on the other side and used for quick adjustment
     * To send information about update use {@link EaselContainerMenu#sendPaletteUpdatePacket}
     * @param paletteSlot
     * @param color
     */
    public void setPaletteColor(int paletteSlot, int color) {
        ItemStack paletteStack = this.easelContainer.getPaletteStack();

        if (paletteStack.isEmpty()) {
            return;
        }

        PaletteItem.updatePaletteColor(paletteStack, paletteSlot, color);
        this.easelContainer.setChanged();
    }

    public void sendPaletteUpdatePacket(int paletteSlot, int color) {
        if (!this.isPaletteAvailable()) {
            return;
        }

        CPaletteUpdatePacket paletteUpdatePacket = new CPaletteUpdatePacket(paletteSlot, color);
        Zetter.LOG.debug("Sending Palette Update: " + paletteUpdatePacket);
        ModNetwork.simpleChannel.sendToServer(paletteUpdatePacket);
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
        this.checkFrameBuffer();
        this.dropOldFrames();
    }

    /**
     * Handle screen event - only on client. On server canvas is modified by handling frames
     * @param pixelX
     * @param pixelY
     * @param color
     */
    public void writePixelOnCanvasClientSide(int pixelX, int pixelY, int color, UUID playerId) {
        if (!this.isCanvasAvailable()) {
            // Nothing to draw on
            return;
        }

        int index = this.canvas.data.getPixelIndex(pixelX, pixelY);

        if (this.writePixelOnCanvas(index, color)) {
            this.checkFrameBuffer();
            this.getCanvasChanges().writeChange(this.world.getGameTime(), index, color);

            PaintingFrame newFrame = new PaintingFrame(System.currentTimeMillis(), (short) index, color, playerId);
            this.placeFrame(newFrame);
            this.updateTextureClient();
        }
    }

    public void eyedropper(int slotIndex, int pixelX, int pixelY) {
        if (!this.isCanvasAvailable()) {
            return;
        }

        int newColor = this.canvas.data.getColorAt(pixelX, pixelY);

        this.setPaletteColor(slotIndex, newColor);
        this.sendPaletteUpdatePacket(slotIndex, newColor);
    }

    public void bucket(int pixelX, int pixelY, int color) {
        if (!this.isCanvasAvailable()) {
            return;
        }

        int position = this.canvas.data.getPixelIndex(pixelX, pixelY);

        CCanvasBucketToolPacket bucketToolPacket = new CCanvasBucketToolPacket(position, color);
        Zetter.LOG.debug("Sending Bucket Tool Packet: " + bucketToolPacket);
        ModNetwork.simpleChannel.sendToServer(bucketToolPacket);
    }

    /**
     * Immediately update texture for client - not needed for server cause no renderer used here
     */
    protected void updateTextureClient() {
        if (this.canvas == null) {
            Zetter.LOG.error("Attempted to update texture with no canvas loaded in easel");
            return;
        }

        CanvasRenderer.getInstance().updateCanvasTexture(this.canvas.code, this.canvas.data);
    }

    /**
     * @todo: Move to TE as Container created per-player
     *
     * @param index
     * @param color
     * @return
     */
    private void writePixelOnCanvasServerSide(int index, int color) {
        this.writePixelOnCanvas(index, color); // do nothing for now

        // @todo: chceck where damage applied in vanilla
        this.easelContainer.getPaletteStack().hurtAndBreak(1, this.player, (player) -> {});
    }

    private boolean writePixelOnCanvas(int index, int color) {
        if (!this.isCanvasAvailable()) {
            // Most times checking twice, not sure how to avoid
            return false;
        }

        if (!this.isPaletteAvailable()) {
            return false;
        }

        if (this.canvas.data.getColorAt(index) == color) {
            // Pixel is not changed
            return false;
        }

        this.getCanvasData().updateCanvasPixel(index, color);

        return true;
    }

    /**
     * Checks and sends frame buffer if it's full or it's time to
     * Should be called before every change
     *
     * Just updating frame start time for prepared buffer if nothing happens
     */
    protected void checkFrameBuffer() {
        if (this.getCanvasChanges().isEmpty()) {
            try {
                this.canvasChanges.updateStartFrameTime(System.currentTimeMillis());
            } catch (Exception e) {
                Zetter.LOG.error("Cannot update Painting Frame Buffer start time: " + e.getMessage());
            }

            return;
        }

        if (this.canvasChanges.shouldBeSent(System.currentTimeMillis())) {
            if (this.world.isClientSide()) {
                this.canvasChanges.getFrames(this.player.getUUID());
                CPaintingFrameBufferPacket paintingFrameBufferPacket = new CPaintingFrameBufferPacket(this.canvasChanges);
                ModNetwork.simpleChannel.sendToServer(paintingFrameBufferPacket);

                this.lastFrameBufferSendClock = System.currentTimeMillis();
            } else {
                Zetter.LOG.warn("Unnecessary Painting Frame Buffer check on server");
            }

            // It'll be created on request next time
            this.canvasChanges = null;
        }
    }

    protected PaintingFrameBuffer getCanvasChanges() {
        if (this.canvasChanges == null) {
            this.canvasChanges = new PaintingFrameBuffer(System.currentTimeMillis());
        }

        return this.canvasChanges;
    }

    /**
     * Called from network - process player's work (only on server)
     * @param paintingFrameBuffer
     */
    public void processFrameBufferServer(PaintingFrameBuffer paintingFrameBuffer, UUID ownerId) {
        long currentTime = this.world.getGameTime();

        for (PaintingFrame frame: paintingFrameBuffer.getFrames(ownerId)) {
            if (currentTime - frame.getFrameTime() > FRAME_TIMEOUT) {
                // Update will be sent to player anyway, and they'll request new sync if they're confused
                Zetter.LOG.debug("Skipping painting frame, too old");
                continue;
            }

            this.writePixelOnCanvasServerSide(frame.getPixelIndex(), frame.getColor());
        }

        ((CanvasServerTracker) Helper.getWorldCanvasTracker(this.world)).markCanvasDesync(this.canvas.code);
    }

    public void processBucketToolServer(int position, int bucketColor) {
        final int width = this.canvas.data.getWidth();
        final int height = this.canvas.data.getHeight();
        final int length = width * height;
        final int replacedColor = this.canvas.data.getColorAt(position);

        LinkedList<Integer> positionsQueue = new LinkedList<>();
        Vector<Integer> checkedQueue = new Vector<>();
        Vector<Integer> paintQueue = new Vector<>();

        positionsQueue.add(position);
        paintQueue.add(position);

        do {
            getNeighborPositions(positionsQueue.pop(), width, length)
                    // Ignore checked positions if overlap
                    .filter(currentPosition -> !checkedQueue.contains(currentPosition))
                    .forEach(currentPosition -> {
                        if (this.canvas.data.getColorAt(currentPosition) == replacedColor) {
                            positionsQueue.add(currentPosition);
                            paintQueue.add(currentPosition);
                        }

                        checkedQueue.add(currentPosition);
                    });
        } while (!positionsQueue.isEmpty());

        for (int updatePosition: paintQueue) {
            this.writePixelOnCanvasServerSide(updatePosition, bucketColor);
        }

        ((CanvasServerTracker) Helper.getWorldCanvasTracker(this.world)).markCanvasDesync(this.canvas.code);
    }

    public static Stream<Integer> getNeighborPositions(int currentCenter, int width, int length) {
        List<Integer> neighborPositions = new ArrayList<>(4);

        final int topPosition = currentCenter - width;
        if (topPosition >= 0) {
            neighborPositions.add(topPosition);
        }

        final int leftPosition = currentCenter - 1;
        // on a single row
        if (leftPosition >= 0 && leftPosition / width == currentCenter / width) {
            neighborPositions.add(leftPosition);
        }

        final int rightPosition = currentCenter + 1;
        // on a single row
        if (rightPosition < length && rightPosition / width == currentCenter / width) {
            neighborPositions.add(rightPosition);
        }

        final int bottomPosition = currentCenter + width;
        if (bottomPosition < length) {
            neighborPositions.add(bottomPosition);
        }

        return neighborPositions.stream();
    }

    /**
     * Add all newest pixels to the canvas when syncing to keep recent player's changes
     * @param canvasData
     * @param timestamp
     */
    @OnlyIn(Dist.CLIENT)
    public void processSync(String canvasCode, CanvasData canvasData, long packetTimestamp) {
        long timestamp = System.currentTimeMillis();

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

        for (PaintingFrame oldFrame: this.lastFrames) {
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

        this.lastSyncReceivedClock = System.currentTimeMillis();
    }

    protected void placeFrame(PaintingFrame frame) {
        this.lastFrames.add(frame);
        this.lastPushedFrameClock = System.currentTimeMillis();
    }

    protected void dropOldFrames() {
        long minTime = Util.getMillis() - FRAME_TIMEOUT * 50;

        for (Iterator<PaintingFrame> iterator = this.lastFrames.iterator(); iterator.hasNext(); ) {
            PaintingFrame oldFrame = iterator.next();
            if (oldFrame.getFrameTime() < minTime) {
                iterator.remove();
            } else {
                // later elements supposed to be newer
                return;
            }
        }
    }

    /*
      Common handlers
     */

    /**
     * Called when the container is closed.
     * Push painting frames so it will be saved
     */
    public void removed(Player playerIn) {
        super.removed(playerIn);

        if (this.world.isClientSide() && !this.getCanvasChanges().isEmpty()) {
            this.canvasChanges.getFrames(playerIn.getUUID());
            CPaintingFrameBufferPacket modePacket = new CPaintingFrameBufferPacket(this.canvasChanges);
            ModNetwork.simpleChannel.sendToServer(modePacket);

            this.lastFrameBufferSendClock = System.currentTimeMillis();
        }
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
        return this.easelContainer.stillValid(player);
    }

    @FunctionalInterface
    public interface Notify {   // Some folks use Runnable, but I prefer not to use it for non-thread-related tasks
        void invoke();
    }
}
