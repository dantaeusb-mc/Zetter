package com.dantaeusb.immersivemp.locks.inventory.container;

import com.dantaeusb.immersivemp.ImmersiveMp;
import com.dantaeusb.immersivemp.locks.capability.canvastracker.CanvasTrackerCapability;
import com.dantaeusb.immersivemp.locks.capability.canvastracker.ICanvasTracker;
import com.dantaeusb.immersivemp.locks.client.gui.CanvasRenderer;
import com.dantaeusb.immersivemp.locks.core.ModLockContainers;
import com.dantaeusb.immersivemp.locks.core.ModLockItems;
import com.dantaeusb.immersivemp.locks.core.ModLockNetwork;
import com.dantaeusb.immersivemp.locks.inventory.container.painting.PaintingFrame;
import com.dantaeusb.immersivemp.locks.inventory.container.painting.PaintingFrameBuffer;
import com.dantaeusb.immersivemp.locks.item.CanvasItem;
import com.dantaeusb.immersivemp.locks.network.packet.painting.SCanvasNamePacket;
import com.dantaeusb.immersivemp.locks.network.packet.painting.PaintingFrameBufferPacket;
import com.dantaeusb.immersivemp.locks.tileentity.storage.EaselStorage;
import com.dantaeusb.immersivemp.locks.world.storage.CanvasData;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import java.nio.ByteBuffer;
import java.util.*;

public class EaselContainer extends Container {
    public static int FRAME_TIMEOUT = 20 * 5;  // 5 second limit to keep changes buffer, if packet processed later disregard it

    private final World world;

    public static final int PALETTE_SLOTS = 14;
    private static final int HOTBAR_SLOT_COUNT = 9;

    private boolean canvasAvailable = false;
    private CanvasData canvas;
    private ByteBuffer palette = ByteBuffer.allocateDirect(PALETTE_SLOTS * 4);

    private final EaselStorage easelStorage;

    // Only player's buffer on client, all users on server
    private HashMap<UUID, PaintingFrameBuffer> frameBuffers = new HashMap<>();
    private LinkedList<PaintingFrame> lastFrames = new LinkedList<>();

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

        this.world = invPlayer.player.world;
        this.easelStorage = easelStorage;

        final int PALETTE_SLOT_X_SPACING = 152;
        final int PALETTE_SLOT_Y_SPACING = 94;

        this.addSlot(new Slot(this.easelStorage, 1, PALETTE_SLOT_X_SPACING, PALETTE_SLOT_Y_SPACING));

        if (!this.world.isRemote()) {
            ItemStack canvasStack = this.easelStorage.getStackInSlot(EaselStorage.CANVAS_SLOT);
            String canvasName = CanvasItem.getCanvasName(canvasStack);
            this.setCanvas(canvasName);
        }

        // Add the players hotbar to the gui - the [xpos, ypos] location of each item
        /*for (int x = 0; x < HOTBAR_SLOT_COUNT; x++) {
            int slotNumber = x;
            addSlot(new Slot(invPlayer, slotNumber, HOTBAR_XPOS + SLOT_X_SPACING * x, HOTBAR_YPOS));
        }*/

        this.tempPropagateData();
    }

    public void setCanvas(String canvasName) {
        if (canvasName.isEmpty()) {
            this.canvas = null;
            this.canvasAvailable = false;
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
            this.canvasAvailable = false;
            return;
        }

        CanvasData canvas = canvasTracker.getCanvasData(canvasName);

        if (canvas == null) {
            this.canvas = null;
            this.canvasAvailable = false;
            return;
        }

        this.canvas = canvas;
        this.canvasAvailable = true;
    }

    protected void tempPropagateData() {
        this.setColor(this.palette, 0, 0xFFAA0000); //dark-red
        this.setColor(this.palette, 1, 0xFFFF5555); //red
        this.setColor(this.palette, 2, 0xFFFFAA00); //gold
        this.setColor(this.palette, 3, 0xFFFFFF55); //yellow
        this.setColor(this.palette, 4, 0xFF00AA00); //dark-green
        this.setColor(this.palette, 5, 0xFF55FF55); //green
        this.setColor(this.palette, 6, 0xFF55FFFF); //aqua
        this.setColor(this.palette, 7, 0xFF00AAAA); //dark-aqua
        this.setColor(this.palette, 8, 0xFF0000AA); //dark-blue
        this.setColor(this.palette, 9, 0xFF5555FF); //blue
        this.setColor(this.palette, 10, 0xFFFF55FF); //light-purple
        this.setColor(this.palette, 11, 0xFFAA00AA); //purple
        this.setColor(this.palette, 12, 0xFFAAAAAA); //gray
        this.setColor(this.palette, 13, 0xFF555555); //dark-gray
    }

    public ByteBuffer getPalette() {
        return this.palette;
    }

    public CanvasData getCanvasData() {
        return this.canvas;
    }

    public boolean isCanvasAvailable() {
        return this.canvasAvailable;
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
        if (!this.canvasAvailable) {
            // Nothing to draw on
            return;
        }

        int index = this.canvas.getPixelIndex(pixelX, pixelY);

        if (this.writePixelOnCanvas(index, color)) {
            this.checkFrameBuffer();
            this.getFrameBuffer(playerId).writeChange(this.world.getGameTime(), index, color);
            this.updateTextureClient();
        }
    }

    /**
     * Immediately update texture for client - not needed for server cause no renderer used here
     */
    protected void updateTextureClient() {
        CanvasRenderer.getInstance().updateCanvas(this.canvas);
    }

    /**
     * Todo: notify all clients about that
     * @param index
     * @param color
     * @return
     */
    private void writePixelOnCanvasServerSide(int index, int color) {
        if (this.writePixelOnCanvas(index, color)) {
            //this.checkFrameBuffer();
        }
    }

    private boolean writePixelOnCanvas(int index, int color) {
        if (!this.canvasAvailable) {
            return false;
        }

        if (this.canvas.getColorAt(index) == color) {
            // Pixel is not changed
            return false;
        }

        this.getCanvasData().updateCanvasPixel(index, color);

        return true;
    }

    protected PaintingFrameBuffer getFrameBuffer(UUID playerId) {
        // Adding buffer if there's none with player's ID
        // They'll be removed in case if it's empty
        if (!this.frameBuffers.containsKey(playerId)) {
            this.frameBuffers.put(playerId, new PaintingFrameBuffer(this.world.getGameTime()));
        }

        return this.frameBuffers.get(playerId);
    }

    /**
     * Checks and sends frame buffer if it's full or it's time to
     * Should be called before every change
     *
     * Just updating frame start time for prepared buffer if nothing happens
     */
    protected void checkFrameBuffer() {
        Iterator<Map.Entry<UUID, PaintingFrameBuffer>> entryIterator = this.frameBuffers.entrySet().iterator();
        while (entryIterator.hasNext()) {
            Map.Entry<UUID, PaintingFrameBuffer> pair = entryIterator.next();
            UUID playerId = pair.getKey();
            PaintingFrameBuffer frameBuffer = pair.getValue();

            if (frameBuffer.isEmpty()) {
                try {
                    frameBuffer.updateStartFrameTime(this.world.getGameTime());
                } catch (Exception e) {
                    ImmersiveMp.LOG.error("Cannot update Painting Frame Buffer start time: " + e.getMessage());
                }

                return;
            }

            if (frameBuffer.shouldBeSent(this.world.getGameTime())) {
                if (this.world.isRemote()) {
                    frameBuffer.getFrames(playerId);
                    PaintingFrameBufferPacket modePacket = new PaintingFrameBufferPacket(frameBuffer);
                    ImmersiveMp.LOG.info("Sending Painting Frame Buffer: " + modePacket);
                    ModLockNetwork.simpleChannel.sendToServer(modePacket);
                } else {
                    //PaintingFrameBufferPacket modePacket = new PaintingFrameBufferPacket(frameBuffer);
                    //PlayerEntity playerEntity = this.world.getPlayerByUuid(playerId);
                    //ModLockNetwork.simpleChannel.send(PacketDistributor.PLAYER.with(playerEntity), modePacket);
                }

                // It'll be created on request next time
                entryIterator.remove();
            }
        }
    }

    /**
     * Called from network - process player's work (
     * @param paintingFrameBuffer
     */
    public void processFrameBuffer(PaintingFrameBuffer paintingFrameBuffer, UUID ownerId) {
        long currentTime = this.world.getGameTime();

        for (PaintingFrame frame: paintingFrameBuffer.getFrames(ownerId)) {
            if (currentTime - frame.getFrameTime() > FRAME_TIMEOUT) {
                this.invalidCache = true;
                ImmersiveMp.LOG.warn("Skipping painting frame, too old");
                continue;
            }

            // Check if the new packed claims to be older than the last processed frame
            if (frame.getFrameTime() <= this.lastFrameTime) {
                // If two players changed same pixel but new packet claims to be older than processed, let's sync player's canvases
                for (PaintingFrame oldFrame: this.lastFrames) {
                    if (oldFrame.getPixelIndex() == frame.getPixelIndex() && oldFrame.getColor() != frame.getColor() && oldFrame.getFrameTime() >= frame.getFrameTime()) {
                        this.invalidCache = true;
                        break;
                    }
                }

                if (this.invalidCache) {
                    ImmersiveMp.LOG.warn("Two clients changed same pixel in unknown order, syncing");
                    continue;
                }
            }

            this.placeFrame(frame);
            this.writePixelOnCanvasServerSide(frame.getPixelIndex(), frame.getColor());
            this.lastFrameTime = frame.getFrameTime();
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

    public void setColor(ByteBuffer byteBuffer, int offset, int color) {
        byteBuffer.putInt(offset * 4, color);
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
