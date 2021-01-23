package com.dantaeusb.immersivemp.locks.inventory.container;

import com.dantaeusb.immersivemp.ImmersiveMp;
import com.dantaeusb.immersivemp.locks.core.ModLockContainers;
import com.dantaeusb.immersivemp.locks.core.ModLockNetwork;
import com.dantaeusb.immersivemp.locks.inventory.container.painting.PaintingFrame;
import com.dantaeusb.immersivemp.locks.inventory.container.painting.PaintingFrameBuffer;
import com.dantaeusb.immersivemp.locks.network.packet.painting.PaintingFrameBufferPacket;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.util.IWorldPosCallable;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.network.PacketDistributor;

import java.nio.ByteBuffer;
import java.util.*;

public class PaintingContainer extends Container {
    protected final IInventory inventoryIn = new Inventory(1) {
        /**
         * @see net.minecraft.inventory.container.RepairContainer#field_234643_d_
         */
        public void markDirty() {
            super.markDirty();
        }
    };

    public static int FRAME_TIMEOUT = 20 * 5;  // 5 second limit to keep changes buffer, if packet processed later disregard it

    private final World world;

    public static final int PALETTE_SLOTS = 14;
    private static final int HOTBAR_SLOT_COUNT = 9;

    protected final IWorldPosCallable worldPosCallable;

    private final ByteBuffer canvas = ByteBuffer.allocateDirect(16 * 16 * 4);
    private final ByteBuffer palette = ByteBuffer.allocateDirect(PALETTE_SLOTS * 4);

    // Only player's buffer on client, all users on server
    private HashMap<UUID, PaintingFrameBuffer> frameBuffers = new HashMap<>();
    private LinkedList<PaintingFrame> lastFrames = new LinkedList<>();

    private boolean invalidCache = false;
    private long lastFrameTime;

    public PaintingContainer(int id, PlayerInventory playerInventory) {
        this(id, playerInventory, IWorldPosCallable.DUMMY);
    }

    public PaintingContainer(int windowID, PlayerInventory invPlayer, IWorldPosCallable worldPosCallable) {
        super(ModLockContainers.PAINTING, windowID);

        if (ModLockContainers.PAINTING == null)
            throw new IllegalStateException("Must initialise containerTypeLockTableContainer before constructing a LockTableContainer!");

        this.worldPosCallable = worldPosCallable;
        this.world = invPlayer.player.world;

        final int SLOT_X_SPACING = 18;
        final int SLOT_Y_SPACING = 18;
        final int HOTBAR_XPOS = 8;
        final int HOTBAR_YPOS = 142;

        // Add the players hotbar to the gui - the [xpos, ypos] location of each item
        /*for (int x = 0; x < HOTBAR_SLOT_COUNT; x++) {
            int slotNumber = x;
            addSlot(new Slot(invPlayer, slotNumber, HOTBAR_XPOS + SLOT_X_SPACING * x, HOTBAR_YPOS));
        }*/

        this.tempPropagateData();
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

        for (int i = 0; i < 16 * 16; i++) {
            this.canvas.putInt(i * 4, 0xFF000000);
        }
    }

    public static PaintingContainer createContainerServerSide(int windowID, PlayerInventory playerInventory) {
        return new PaintingContainer(windowID, playerInventory);
    }

    public static PaintingContainer createContainerClientSide(int windowID, PlayerInventory playerInventory, net.minecraft.network.PacketBuffer extraData) {
        return new PaintingContainer(windowID, playerInventory);
    }

    public ByteBuffer getPalette() {
        return this.palette;
    }

    public ByteBuffer getCanvas() {
        return this.canvas;
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
    public void writePixelOnCanvasClient(int pixelX, int pixelY, int color, UUID playerId) {
        int index = this.getPixelIndex(pixelX, pixelY);
        if (this.writePixelOnCanvas(index, color)) {
            this.checkFrameBuffer();
            this.getFrameBuffer(playerId).writeChange(this.world.getGameTime(), index, color);
        }
    }

    private boolean writePixelOnCanvas(int index, int color) {
        if (this.getCanvas().getInt(index) == color) {
            // Pixel is not changed
            return false;
        }

        this.getCanvas().putInt(index, color);
        return true;
    }

    private int getPixelIndex(int pixelX, int pixelY) {
        pixelX = MathHelper.clamp(pixelX, 0, 15);
        pixelY = MathHelper.clamp(pixelY, 0, 15);

        int index = pixelY * 16 + pixelX;
        index *= 4;

        return index;
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
                    ImmersiveMp.LOG.info(frameBuffer.getFrameStartTime());
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

            if (frame.getFrameTime() <= this.lastFrameTime) {
                for (PaintingFrame oldFrame: this.lastFrames) {
                    if (oldFrame.getPixelIndex() == frame.getPixelIndex()) {
                        this.invalidCache = true;
                    }
                }

                if (this.invalidCache) {
                    ImmersiveMp.LOG.warn("Two clients changed same pixel in unknown order, syncing");
                    continue;
                }
            }

            this.placeFrame(frame);
            this.writePixelOnCanvas(frame.getPixelIndex(), frame.getColor());
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

    /**
     * @todo remove transparency, add to bytebuffer.
     * @param byteBuffer
     * @param offset
     * @return
     */
    public int getColor(ByteBuffer byteBuffer, int offset) {
        return byteBuffer.getInt(offset * 4);
    }

    public void setColor(ByteBuffer byteBuffer, int offset, int color) {
        byteBuffer.putInt(offset * 4, color);
    }

    /**
     * Called when the container is closed.
     */
    public void onContainerClosed(PlayerEntity playerIn) {
        super.onContainerClosed(playerIn);
        this.worldPosCallable.consume((worldIn, blockPos) -> {
            this.clearContainer(playerIn, worldIn, this.inventoryIn);
        });
    }

    /**
     * Determines whether supplied player can use this container
     */
    public boolean canInteractWith(PlayerEntity playerIn) {
        return this.worldPosCallable.applyOrElse((worldPosConsumer, defaultValue) -> {
            return !this.isAnLockTable(worldPosConsumer.getBlockState(defaultValue)) ? false : playerIn.getDistanceSq((double)defaultValue.getX() + 0.5D, (double)defaultValue.getY() + 0.5D, (double)defaultValue.getZ() + 0.5D) <= 64.0D;
        }, true);
    }

    /**
     * @todo Use BlockTags {@link net.minecraft.inventory.container.RepairContainer#func_230302_a_}
     * @param blockState
     * @return
     */
    protected boolean isAnLockTable(BlockState blockState) {
        return true;
    }
}
