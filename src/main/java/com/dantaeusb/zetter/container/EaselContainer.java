package com.dantaeusb.zetter.container;

import com.dantaeusb.zetter.Zetter;
import com.dantaeusb.zetter.canvastracker.CanvasServerTracker;
import com.dantaeusb.zetter.canvastracker.CanvasTrackerCapability;
import com.dantaeusb.zetter.canvastracker.ICanvasTracker;
import com.dantaeusb.zetter.client.renderer.CanvasRenderer;
import com.dantaeusb.zetter.core.Helper;
import com.dantaeusb.zetter.core.ModContainers;
import com.dantaeusb.zetter.core.ModItems;
import com.dantaeusb.zetter.core.ModNetwork;
import com.dantaeusb.zetter.container.painting.PaintingFrame;
import com.dantaeusb.zetter.container.painting.PaintingFrameBuffer;
import com.dantaeusb.zetter.item.CanvasItem;
import com.dantaeusb.zetter.item.PaletteItem;
import com.dantaeusb.zetter.network.packet.*;
import com.dantaeusb.zetter.tileentity.EaselTileEntity;
import com.dantaeusb.zetter.tileentity.storage.EaselStorage;
import com.dantaeusb.zetter.storage.CanvasData;
import javafx.util.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.play.NetworkPlayerInfo;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Util;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Stream;

public class EaselContainer extends Container {
    public static int FRAME_TIMEOUT = 5000;  // 5 second limit to keep changes buffer, if packet processed later disregard it

    private final PlayerEntity player;
    private final World world;

    public static final int PALETTE_SLOTS = 14;
    private static final int HOTBAR_SLOT_COUNT = 9;

    private boolean canvasReady = false;
    private CanvasData canvas;

    private EaselTileEntity tileEntity;
    private final EaselStorage easelStorage;

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

    public EaselContainer(int windowID, PlayerInventory invPlayer, EaselStorage easelStorage) {
        super(ModContainers.PAINTING, windowID);

        if (ModContainers.PAINTING == null)
            throw new IllegalStateException("Must initialise containerTypeLockTableContainer before constructing a LockTableContainer!");

        this.player = invPlayer.player;
        this.world = invPlayer.player.world;
        this.easelStorage = easelStorage;
        this.canvasChanges = new PaintingFrameBuffer(System.currentTimeMillis());

        final int PALETTE_SLOT_X_SPACING = 152;
        final int PALETTE_SLOT_Y_SPACING = 94;

        final int HOTBAR_XPOS = 8;
        final int HOTBAR_YPOS = 161;
        final int SLOT_X_SPACING = 18;

        this.addSlot(new Slot(this.easelStorage, 1, PALETTE_SLOT_X_SPACING, PALETTE_SLOT_Y_SPACING) {
            public boolean isItemValid(ItemStack stack) {
                return stack.getItem() == ModItems.PALETTE;
            }
        });

        if (!this.world.isRemote()) {
            ItemStack canvasStack = this.easelStorage.getStackInSlot(EaselStorage.CANVAS_SLOT);
            String canvasName = CanvasItem.getCanvasCode(canvasStack);
            this.setCanvas(canvasName);
        }

        // Add the players hotbar to the gui - the [xpos, ypos] location of each item
        for (int x = 0; x < HOTBAR_SLOT_COUNT; x++) {
            int slotNumber = x;
            addSlot(new Slot(invPlayer, slotNumber, HOTBAR_XPOS + SLOT_X_SPACING * x, HOTBAR_YPOS));
        }
    }

    public static EaselContainer createContainerServerSide(int windowID, PlayerInventory playerInventory, EaselTileEntity tileEntity, EaselStorage easelStorage) {
        EaselContainer easelContainer = new EaselContainer(windowID, playerInventory, easelStorage);

        easelContainer.tileEntity = tileEntity;
        easelStorage.setMarkDirtyNotificationLambda(easelContainer::markDirty);

        return easelContainer;
    }

    public static EaselContainer createContainerClientSide(int windowID, PlayerInventory playerInventory, net.minecraft.network.PacketBuffer networkBuffer) {
        // it seems like we have to utilize extraData to pass the canvas data
        // slots seems to be synchronised, but we would prefer to avoid that to prevent every-pixel sync
        EaselStorage easelStorage = EaselStorage.createForClientSideContainer();

        String canvasName = SCanvasNamePacket.readCanvasName(networkBuffer);

        EaselContainer easelContainer = new EaselContainer(windowID, playerInventory, easelStorage);
        easelContainer.setCanvas(canvasName);

        return easelContainer;
    }

    @Nullable
    public EaselTileEntity getTileEntityReference() {
        return this.tileEntity;
    }

    public EaselStorage getEaselStorage() {
        return this.easelStorage;
    }

    /**
     * Because we don't have a special Slot for the canvas, we're using custom
     * update functions and network message
     */

    public void setFirstLoadNotification(Notify firstLoadNotification) {
        this.firstLoadNotification = firstLoadNotification;
    }

    public void markDirty() {
        SEaselCanvasChangePacket canvasSyncMessage = new SEaselCanvasChangePacket(this.windowId, this.easelStorage.getCanvasStack());
        ModNetwork.simpleChannel.send(PacketDistributor.PLAYER.with(() -> (ServerPlayerEntity) this.player), canvasSyncMessage);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void setAll(List<ItemStack> itemStacks) {
        super.setAll(itemStacks);

        if (this.firstLoadNotification != null) {
            this.firstLoadNotification.invoke();
        }
    }

    public void handleCanvasChange(ItemStack canvasStack) {
        if (canvasStack.getItem() == ModItems.CANVAS) {
            this.canvas = CanvasItem.getCanvasData(canvasStack, this.world);
            this.canvasReady = true;
        } else {
            this.canvas = null;
            this.canvasReady = false;
        }

        if (this.firstLoadNotification != null) {
            this.firstLoadNotification.invoke();
        }
    }

    public void setCanvas(String canvasName) {
        if (canvasName.isEmpty() || canvasName.equals(CanvasData.getCanvasCode(0))) {
            this.canvas = null;
            this.canvasReady = false;
            return;
        }

        ICanvasTracker canvasTracker;

        if (this.world.isRemote()) {
            canvasTracker = this.world.getCapability(CanvasTrackerCapability.CAPABILITY_CANVAS_TRACKER).orElse(null);
        } else {
            canvasTracker = this.world.getServer().func_241755_D_().getCapability(CanvasTrackerCapability.CAPABILITY_CANVAS_TRACKER).orElse(null);
        }

        if (canvasTracker == null) {
            Zetter.LOG.error("Cannot find world canvas capability");
            this.canvas = null;
            this.canvasReady = false;
            return;
        }

        CanvasData canvas = canvasTracker.getCanvasData(canvasName, CanvasData.class);

        if (canvas == null) {
            this.canvas = null;
            this.canvasReady = false;
            return;
        }

        this.canvas = canvas;
        this.canvasReady = true;
    }

    public int getPaletteColor(int paletteSlot) {
        ItemStack paletteStack = this.easelStorage.getPaletteStack();

        if (paletteStack.isEmpty()) {
            return 0xFF000000;
        }

        return PaletteItem.getPaletteColors(paletteStack)[paletteSlot];
    }

    /**
     * This won't update palette color on the other side and used for quick adjustment
     * To send information about update use {@link EaselContainer#sendPaletteUpdatePacket}
     * @param paletteSlot
     * @param color
     */
    public void setPaletteColor(int paletteSlot, int color) {
        ItemStack paletteStack = this.easelStorage.getPaletteStack();

        if (paletteStack.isEmpty()) {
            return;
        }

        PaletteItem.updatePaletteColor(paletteStack, paletteSlot, color);
        this.easelStorage.markDirty();
    }

    public void sendPaletteUpdatePacket(int paletteSlot, int color) {
        if (!this.isPaletteAvailable()) {
            return;
        }

        CPaletteUpdatePacket paletteUpdatePacket = new CPaletteUpdatePacket(paletteSlot, color);
        Zetter.LOG.info("Sending Palette Update: " + paletteUpdatePacket);
        ModNetwork.simpleChannel.sendToServer(paletteUpdatePacket);
    }

    public CanvasData getCanvasData() {
        return this.canvas;
    }

    public boolean isCanvasAvailable() {
        return this.canvasReady;
    }

    public boolean isPaletteAvailable() {
        ItemStack paletteStack = this.easelStorage.getPaletteStack();

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
        if (!this.canvasReady) {
            // Nothing to draw on
            return;
        }

        int index = this.canvas.getPixelIndex(pixelX, pixelY);

        if (this.writePixelOnCanvas(index, color)) {
            this.checkFrameBuffer();
            this.getCanvasChanges().writeChange(this.world.getGameTime(), index, color);

            PaintingFrame newFrame = new PaintingFrame(System.currentTimeMillis(), (short) index, color, playerId);
            this.placeFrame(newFrame);
            this.updateTextureClient();
        }
    }

    public void eyedropper(int slotIndex, int pixelX, int pixelY) {
        int newColor = this.canvas.getColorAt(pixelX, pixelY);

        this.setPaletteColor(slotIndex, newColor);
        this.sendPaletteUpdatePacket(slotIndex, newColor);
    }

    public void bucket(int pixelX, int pixelY, int color) {
        int position = this.canvas.getPixelIndex(pixelX, pixelY);

        CCanvasBucketToolPacket bucketToolPacket = new CCanvasBucketToolPacket(position, color);
        Zetter.LOG.info("Sending Bucket Tool Packet: " + bucketToolPacket);
        ModNetwork.simpleChannel.sendToServer(bucketToolPacket);
    }

    /**
     * Immediately update texture for client - not needed for server cause no renderer used here
     */
    protected void updateTextureClient() {
        CanvasRenderer.getInstance().updateCanvasTexture(this.canvas);
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
        this.easelStorage.getPaletteStack().damageItem(1, this.player, (player) -> {});
    }

    private boolean writePixelOnCanvas(int index, int color) {
        if (!this.canvasReady) {
            // Most times checking twice, not sure how to avoid
            return false;
        }

        if (!this.isPaletteAvailable()) {
            return false;
        }

        if (this.canvas.getColorAt(index) == color) {
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
            if (this.world.isRemote()) {
                this.canvasChanges.getFrames(this.player.getUniqueID());
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
                Zetter.LOG.info("Skipping painting frame, too old");
                continue;
            }

            this.writePixelOnCanvasServerSide(frame.getPixelIndex(), frame.getColor());
        }

        ((CanvasServerTracker) Helper.getWorldCanvasTracker(this.world)).markCanvasDesync(this.canvas.getName());
    }

    public void processBucketToolServer(int position, int bucketColor) {
        final int width = this.canvas.getWidth();
        final int height = this.canvas.getHeight();
        final int length = width * height;
        final int replacedColor = this.canvas.getColorAt(position);

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
                        if (this.canvas.getColorAt(currentPosition) == replacedColor) {
                            positionsQueue.add(currentPosition);
                            paintQueue.add(currentPosition);
                        }

                        checkedQueue.add(currentPosition);
                    });
        } while (!positionsQueue.isEmpty());

        for (int updatePosition: paintQueue) {
            this.writePixelOnCanvasServerSide(updatePosition, bucketColor);
        }

        ((CanvasServerTracker) Helper.getWorldCanvasTracker(this.world)).markCanvasDesync(this.canvas.getName());
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
    public void processSync(CanvasData canvasData, long packetTimestamp) {
        long timestamp = System.currentTimeMillis();

        NetworkPlayerInfo playerInfo = Minecraft.getInstance().getConnection().getPlayerInfo(this.player.getUniqueID());
        int latency = playerInfo.getResponseTime();

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
        Pair<Long, Long> lowTrustInterval = new Pair<>(adjustedTimestamp, timestamp - this.lastFrameBufferSendClock);

        Zetter.LOG.info("Latency: " + latency);

        for (PaintingFrame oldFrame: this.lastFrames) {
            if (oldFrame.getFrameTime() < lowTrustInterval.getKey()) {
                // @todo: reasonable to remove older frames right there
            } else {
                if (oldFrame.getFrameTime() < lowTrustInterval.getValue()) {
                    mightDesync = true;
                }

                canvasData.updateCanvasPixel(oldFrame.getPixelIndex(), oldFrame.getColor());
            }
        }

        if (mightDesync) {
            CanvasRenderer.getInstance().queueCanvasTextureUpdate(canvasData.getType(), canvasData.getName());
        }

        this.canvas = canvasData;

        this.lastSyncReceivedClock = System.currentTimeMillis();
    }

    protected void placeFrame(PaintingFrame frame) {
        this.lastFrames.add(frame);
        this.lastPushedFrameClock = System.currentTimeMillis();
    }

    protected void dropOldFrames() {
        long minTime = Util.milliTime() - FRAME_TIMEOUT * 50;

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
    public void onContainerClosed(PlayerEntity playerIn) {
        super.onContainerClosed(playerIn);

        if (this.world.isRemote() && !this.getCanvasChanges().isEmpty()) {
            this.canvasChanges.getFrames(playerIn.getUniqueID());
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
        return this.easelStorage.isUsableByPlayer(player);
    }

    @FunctionalInterface
    public interface Notify {   // Some folks use Runnable, but I prefer not to use it for non-thread-related tasks
        void invoke();
    }
}
