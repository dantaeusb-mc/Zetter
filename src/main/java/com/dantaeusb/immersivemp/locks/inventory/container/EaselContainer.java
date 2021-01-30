package com.dantaeusb.immersivemp.locks.inventory.container;

import com.dantaeusb.immersivemp.ImmersiveMp;
import com.dantaeusb.immersivemp.locks.capability.canvastracker.CanvasServerTracker;
import com.dantaeusb.immersivemp.locks.capability.canvastracker.CanvasTrackerCapability;
import com.dantaeusb.immersivemp.locks.capability.canvastracker.ICanvasTracker;
import com.dantaeusb.immersivemp.locks.client.gui.CanvasRenderer;
import com.dantaeusb.immersivemp.locks.core.Helper;
import com.dantaeusb.immersivemp.locks.core.ModLockContainers;
import com.dantaeusb.immersivemp.locks.core.ModLockItems;
import com.dantaeusb.immersivemp.locks.core.ModLockNetwork;
import com.dantaeusb.immersivemp.locks.inventory.container.painting.PaintingFrame;
import com.dantaeusb.immersivemp.locks.inventory.container.painting.PaintingFrameBuffer;
import com.dantaeusb.immersivemp.locks.item.CanvasItem;
import com.dantaeusb.immersivemp.locks.item.PaletteItem;
import com.dantaeusb.immersivemp.locks.network.packet.painting.CanvasRequestPacket;
import com.dantaeusb.immersivemp.locks.network.packet.painting.CPaletteUpdatePacket;
import com.dantaeusb.immersivemp.locks.network.packet.painting.SCanvasNamePacket;
import com.dantaeusb.immersivemp.locks.network.packet.painting.PaintingFrameBufferPacket;
import com.dantaeusb.immersivemp.locks.tileentity.storage.EaselStorage;
import com.dantaeusb.immersivemp.locks.world.storage.CanvasData;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.play.ClientPlayNetHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.world.World;

import java.util.*;

public class EaselContainer extends Container {
    public static int FRAME_TIMEOUT = 20 * 5;  // 5 second limit to keep changes buffer, if packet processed later disregard it

    private final PlayerEntity player;
    private final World world;

    public static final int PALETTE_SLOTS = 14;
    private static final int HOTBAR_SLOT_COUNT = 9;

    private boolean canvasReady = false;
    private CanvasData canvas;

    private final EaselStorage easelStorage;

    /**
     * Not needed to be map if there's multiple containers for one TE on server
     */
    // Only player's buffer on client, all users on server
    private PaintingFrameBuffer canvasChanges;

    //
    private PaintingFrameBuffer paletteChanges;

    private final LinkedList<PaintingFrame> lastFrames = new LinkedList<>();

    private boolean invalidCache = false;
    private long lastFrameTime;

    public static EaselContainer createContainerServerSide(int windowID, PlayerInventory playerInventory, EaselStorage easelStorage) {
        return new EaselContainer(windowID, playerInventory, easelStorage);
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

    public EaselContainer(int windowID, PlayerInventory invPlayer, EaselStorage easelStorage) {
        super(ModLockContainers.PAINTING, windowID);

        if (ModLockContainers.PAINTING == null)
            throw new IllegalStateException("Must initialise containerTypeLockTableContainer before constructing a LockTableContainer!");

        this.player = invPlayer.player;
        this.world = invPlayer.player.world;
        this.easelStorage = easelStorage;
        this.canvasChanges = new PaintingFrameBuffer(this.world.getGameTime());

        final int PALETTE_SLOT_X_SPACING = 152;
        final int PALETTE_SLOT_Y_SPACING = 94;

        final int HOTBAR_XPOS = 8;
        final int HOTBAR_YPOS = 161;
        final int SLOT_X_SPACING = 18;

        this.addSlot(new Slot(this.easelStorage, 1, PALETTE_SLOT_X_SPACING, PALETTE_SLOT_Y_SPACING) {
            public boolean isItemValid(ItemStack stack) {
                return stack.getItem() == ModLockItems.PALETTE_ITEM;
            }
        });

        if (!this.world.isRemote()) {
            ItemStack canvasStack = this.easelStorage.getStackInSlot(EaselStorage.CANVAS_SLOT);
            String canvasName = CanvasItem.getCanvasName(canvasStack);
            this.setCanvas(canvasName);
        }

        // Add the players hotbar to the gui - the [xpos, ypos] location of each item
        for (int x = 0; x < HOTBAR_SLOT_COUNT; x++) {
            int slotNumber = x;
            addSlot(new Slot(invPlayer, slotNumber, HOTBAR_XPOS + SLOT_X_SPACING * x, HOTBAR_YPOS));
        }
    }

    public void setCanvas(String canvasName) {
        if (canvasName.isEmpty() || canvasName.equals(CanvasData.getCanvasName(0))) {
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
            ImmersiveMp.LOG.error("Cannot find world canvas capability");
            this.canvas = null;
            this.canvasReady = false;
            return;
        }

        CanvasData canvas = canvasTracker.getCanvasData(canvasName);

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
        ImmersiveMp.LOG.info("Sending Palette Update: " + paletteUpdatePacket);
        ModLockNetwork.simpleChannel.sendToServer(paletteUpdatePacket);
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

            PaintingFrame newFrame = new PaintingFrame(this.world.getGameTime(), (short) index, color, playerId);
            this.placeFrame(newFrame);
            this.updateTextureClient();
        }
    }

    public void eyedropper(int slotIndex, int pixelX, int pixelY) {
        int newColor = this.canvas.getColorAt(pixelX, pixelY);

        this.setPaletteColor(slotIndex, newColor);
        this.sendPaletteUpdatePacket(slotIndex, newColor);
    }

    /**
     * Immediately update texture for client - not needed for server cause no renderer used here
     */
    protected void updateTextureClient() {
        CanvasRenderer.getInstance().updateCanvas(this.canvas);
    }

    /**
     * @param index
     * @param color
     * @return
     */
    private void writePixelOnCanvasServerSide(int index, int color) {
        this.writePixelOnCanvas(index, color); // do nothing for now
    }

    private boolean writePixelOnCanvas(int index, int color) {
        if (!this.canvasReady) {
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
        this.easelStorage.getPaletteStack().damageItem(1, this.player, (player) -> {});

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
                this.canvasChanges.updateStartFrameTime(this.world.getGameTime());
            } catch (Exception e) {
                ImmersiveMp.LOG.error("Cannot update Painting Frame Buffer start time: " + e.getMessage());
            }

            return;
        }

        if (this.canvasChanges.shouldBeSent(this.world.getGameTime())) {
            if (this.world.isRemote()) {
                this.canvasChanges.getFrames(this.player.getUniqueID());
                PaintingFrameBufferPacket modePacket = new PaintingFrameBufferPacket(this.canvasChanges);
                ModLockNetwork.simpleChannel.sendToServer(modePacket);
            } else {
                ImmersiveMp.LOG.warn("Unnecessary Painting Frame Buffer check on server");
                //PaintingFrameBufferPacket modePacket = new PaintingFrameBufferPacket(frameBuffer);
                //PlayerEntity playerEntity = this.world.getPlayerByUuid(playerId);
                //ModLockNetwork.simpleChannel.send(PacketDistributor.PLAYER.with(playerEntity), modePacket);
            }

            // It'll be created on request next time
            this.canvasChanges = null;
        }
    }

    protected PaintingFrameBuffer getCanvasChanges() {
        if (this.canvasChanges == null) {
            this.canvasChanges = new PaintingFrameBuffer(this.world.getGameTime());
        }

        return this.canvasChanges;
    }

    /**
     * Called from network - process player's work (only on server)
     * @param paintingFrameBuffer
     */
    public void processFrameBuffer(PaintingFrameBuffer paintingFrameBuffer, UUID ownerId) {
        long currentTime = this.world.getGameTime();

        for (PaintingFrame frame: paintingFrameBuffer.getFrames(ownerId)) {
            if (currentTime - frame.getFrameTime() > FRAME_TIMEOUT) {
                this.invalidCache = true;
                ImmersiveMp.LOG.info("Skipping painting frame, too old");
                continue;
            }

            // @todo: this doesn't work due to the fact that container is created per-player.
            // Check if the new packed claims to be older than the last processed frame
            if (frame.getFrameTime() <= this.lastFrameTime) {
                // If two players changed same pixel but new packet claims to be older than processed, let's sync player's canvases
                for (PaintingFrame oldFrame: this.lastFrames) {
                    if (oldFrame.getPixelIndex() == frame.getPixelIndex() && oldFrame.getColor() != frame.getColor() && oldFrame.getFrameTime() > frame.getFrameTime()) {
                        this.invalidCache = true;
                        break;
                    }
                }

                // Will never happen, see above
                if (this.invalidCache) {
                    ImmersiveMp.LOG.warn("Two clients changed same pixel in unknown order, syncing");

                    CanvasRequestPacket requestSyncPacket = new CanvasRequestPacket(this.canvas.getName());
                    ModLockNetwork.simpleChannel.sendToServer(requestSyncPacket);

                    continue;
                }
            }

            this.writePixelOnCanvasServerSide(frame.getPixelIndex(), frame.getColor());
            this.lastFrameTime = frame.getFrameTime();
        }

        ((CanvasServerTracker) Helper.getWorldCanvasTracker(this.world)).markCanvasDesync(this.canvas.getName());
    }

    /**
     * Add all newest pixels to the canvas when syncing to keep recent player's changes
     * @param canvasData
     * @param timestamp
     */
    public void processSync(CanvasData canvasData, long timestamp) {
        /**
         * Adjust the time to the extreme case when sync packet was generated at the same time we sent prev frame
         * +10% latency jitter should be enough in most cases. Cause latency works both sides same way, only one
         * latency adjustment seems enough
          */

        // @todo: reduce timeframe by replacing PaintingFrameBuffer.FRAME_TIME_LIMIT with time since last request sent
        int latency = Minecraft.getInstance().getConnection().getPlayerInfo(this.player.getUniqueID()).getResponseTime();
        long adjustedTimestamp = (long) (timestamp - (latency * 1.1f) - PaintingFrameBuffer.FRAME_TIME_LIMIT);
        
        boolean mightDesync = false;

        for (PaintingFrame oldFrame: this.lastFrames) {
            if (oldFrame.getFrameTime() >= adjustedTimestamp) {
                canvasData.updateCanvasPixel(oldFrame.getPixelIndex(), oldFrame.getColor());
                mightDesync = true;
            }
        }

        // @todo: Need some kind of deferred request so it wont spam server
        if (mightDesync) {
            //CanvasRequestPacket requestSyncPacket = new CanvasRequestPacket(this.canvas.getName());
            //ModLockNetwork.simpleChannel.sendToServer(requestSyncPacket);
        }
    }

    protected void placeFrame(PaintingFrame frame) {
        this.lastFrames.add(frame);
    }

    protected void dropOldFrames() {
        long minTime = this.world.getGameTime() - FRAME_TIMEOUT;

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
                if (sourceStack.getItem() == ModLockItems.PALETTE_ITEM) {
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
     * @todo Use BlockTags {@link net.minecraft.inventory.container.RepairContainer#func_230302_a_}
     * @param blockState
     * @return
     */
    protected boolean isAnEasel(BlockState blockState) {
        return true;
    }
}
