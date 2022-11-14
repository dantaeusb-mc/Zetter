package me.dantaeusb.zetter.canvastracker;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.client.renderer.CanvasRenderer;
import me.dantaeusb.zetter.core.ZetterNetwork;
import me.dantaeusb.zetter.core.ZetterRegistries;
import me.dantaeusb.zetter.event.CanvasPostRegisterEvent;
import me.dantaeusb.zetter.event.CanvasPreRegisterEvent;
import me.dantaeusb.zetter.network.packet.SCanvasSyncMessage;
import me.dantaeusb.zetter.storage.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.*;

public class CanvasServerTracker implements ICanvasTracker {
    private static final String NBT_TAG_CANVAS_LAST_ID = "LastCanvasId";
    private static final String NBT_TAG_CANVAS_IDS = "CanvasIds";
    private static final String NBT_TAG_PAINTING_LAST_ID = "LastPaintingId";

    private final Level world;

    protected BitSet canvasIds = new BitSet(1);
    protected int lastPaintingId = 0;

    private final Map<String, Vector<PlayerTrackingCanvas>> trackedCanvases = new HashMap<>();
    private final Vector<String> desyncCanvases = new Vector<>();
    private int ticksFromLastSync = 0;

    public CanvasServerTracker(Level world) {
        super();

        this.world = world;
    }

    @Override
    public Level getWorld() {
        return this.world;
    }

    /*
     * Canvas ids tracked as a bit set, with 1 bit
     * marks ids is used and 0 bit marks id free to use
     * Every time we look for a new id, we seek for the first
     * 0 bit. Every time canvas is combined, signed
     * or removed, we set according bits to 0
     */

    public BitSet getCanvasIds() {
        return this.canvasIds;
    }

    public void setCanvasIds(BitSet canvasIds) {
        this.canvasIds = canvasIds;
    }

    public int getFreeCanvasId() {
        final int freeId = this.canvasIds.nextClearBit(1);
        this.canvasIds.set(freeId);

        return freeId;
    }

    public int getLastCanvasId() {
        return this.canvasIds.length() - 1;
    }

    public void clearCanvasId(int id) {
        this.canvasIds.clear(id);
    }

    /*
     * Paintings are easier, we do not
     * suppose to remove painting data only rarely
     * in exceptional cases and for the mose cases
     * keep data even if the painting is destroyed somehow
     */

    public int getFreePaintingId() {
        return ++this.lastPaintingId ;
    }

    public int getLastPaintingId() {
        return this.lastPaintingId;
    }

    public void setLastPaintingId(int id) {
        this.lastPaintingId = id;
    }

    public void markCanvasDesync(String canvasCode) {
        if (this.desyncCanvases.contains(canvasCode)) {
            // Already waiting for sync
            return;
        }

        this.desyncCanvases.add(canvasCode);
    }

    @Override
    @Nullable
    public <T extends AbstractCanvasData> T getCanvasData(String canvasCode) {
        return this.world.getServer().overworld().getDataStorage().get(
            (compoundTag) -> {
                int canvasTypeInt = -1;
                String canvasType = compoundTag.getString(AbstractCanvasData.NBT_TAG_TYPE);

                if (canvasType.isEmpty()) {
                    if (!compoundTag.contains(AbstractCanvasData.NBT_TAG_TYPE_DEPRECATED)) {
                        throw new IllegalStateException("Cannot find canvas type");
                    }

                    canvasTypeInt = compoundTag.getInt(AbstractCanvasData.NBT_TAG_TYPE_DEPRECATED);

                    switch (canvasTypeInt) {
                        case 0:
                            canvasType = DummyCanvasData.TYPE;
                            break;
                        case 1:
                            canvasType = CanvasData.TYPE;
                            break;
                        case 2:
                            canvasType = PaintingData.TYPE;
                            break;
                    }
                }

                CanvasDataType<T> type = (CanvasDataType<T>) ZetterRegistries.CANVAS_TYPE.get().getValue(new ResourceLocation(Zetter.MOD_ID, canvasType));

                if (type == null) {
                    throw new IllegalStateException("No type of canvas " + canvasType + " is registered");
                }

                T canvasData = type.loadFromNbt(compoundTag);

                // Remove deprecated tags
                if (canvasTypeInt != -1) {
                    canvasData.setDirty();
                }

                return canvasData;
            },
            canvasCode
        );
    }

    /**
     * Just replacing the object that is serialized - maybe may need to be sure
     * that the object is GC'd
     *
     * We can't do that on client cause renderers are using object reference,
     * and changing object would disconnect it from reference
     */
    @Override
    public void registerCanvasData(String canvasCode, AbstractCanvasData canvasData, long timestamp) {
        if (!canvasData.isManaged()) {
            Zetter.LOG.error("Trying to register unmanaged canvas on server side");
            return;
        }

        CanvasPreRegisterEvent preEvent = new CanvasPreRegisterEvent(canvasCode, canvasData, timestamp);
        MinecraftForge.EVENT_BUS.post(preEvent);

        this.world.getServer().overworld().getDataStorage().set(canvasCode, canvasData);

        CanvasPostRegisterEvent postEvent = new CanvasPostRegisterEvent(canvasCode, canvasData, timestamp);
        MinecraftForge.EVENT_BUS.post(postEvent);
    }

    /**
     * Practically, remove painting. We do not do that and
     * do not intend to do that right now.
     *
     * @param canvasCode
     */
    @Override
    public void unregisterCanvasData(String canvasCode) {
        Zetter.LOG.error("Trying to unregister canvas on server side, not supported yet");
    }

    /**
     * Server handling - ticking, tracking from players and syncing
     */

    public void tick() {
        this.ticksFromLastSync++;

        if (this.ticksFromLastSync < 20) {
            return;
        }

        /**
         * Send canvas sync message to every tracking player entity
         */
        MinecraftServer server = this.world.getServer();

        for (String canvasCode : this.desyncCanvases) {
            for (PlayerTrackingCanvas playerTrackingCanvas : this.getTrackingEntries(canvasCode)) {
                ServerPlayer playerEntity = server.getPlayerList().getPlayer(playerTrackingCanvas.playerId);

                SCanvasSyncMessage canvasSyncMessage = new SCanvasSyncMessage(canvasCode, this.getCanvasData(canvasCode), System.currentTimeMillis());
                ZetterNetwork.simpleChannel.send(PacketDistributor.PLAYER.with(() -> playerEntity), canvasSyncMessage);
            }
        }

        this.desyncCanvases.clear();
        this.ticksFromLastSync = 0;
    }

    /**
     * @todo check if already tracking
     * @param playerId
     * @param canvasName
     */
    public void trackCanvas(UUID playerId, String canvasName) {
        Vector<PlayerTrackingCanvas> trackingEntries = this.getTrackingEntries(canvasName);

        for (PlayerTrackingCanvas playerTrackingCanvas : trackingEntries) {
            if (playerTrackingCanvas.playerId == playerId) {
                return;
            }
        }

        trackingEntries.add(new PlayerTrackingCanvas(playerId, canvasName));
    }

    public void stopTrackingCanvas(UUID playerId, String canvasName) {
        Vector<PlayerTrackingCanvas> trackingEntries = this.getTrackingEntries(canvasName);
        trackingEntries.removeIf((PlayerTrackingCanvas entry) -> entry.playerId == playerId);
    }

    public void stopTrackingAllCanvases(UUID playerId) {
        for (String canvasName : this.trackedCanvases.keySet()) {
            this.stopTrackingCanvas(playerId, canvasName);
        }
    }

    private Vector<PlayerTrackingCanvas> getTrackingEntries(String canvasName) {
        return this.trackedCanvases.computeIfAbsent(canvasName, k -> new Vector<>());
    }

    private static class PlayerTrackingCanvas {
        public final UUID playerId;
        public final String canvasName;

        PlayerTrackingCanvas(UUID playerId, String canvasName) {
            this.playerId = playerId;
            this.canvasName = canvasName;
        }
    }

    /*
     * Saving data
     */

    public Tag serializeNBT() {
        CompoundTag compound = new CompoundTag();

        compound.putByteArray(NBT_TAG_CANVAS_IDS, this.getCanvasIds().toByteArray());
        compound.putInt(NBT_TAG_CANVAS_LAST_ID, this.getLastCanvasId());
        compound.putInt(NBT_TAG_PAINTING_LAST_ID, this.getLastPaintingId());

        return compound;
    }

    public void deserializeNBT(Tag tag) {
        if (tag.getType() == CompoundTag.TYPE) {
            CompoundTag compoundTag = (CompoundTag) tag;

            // Backward compat for pre-16
            if (compoundTag.contains(NBT_TAG_CANVAS_IDS)) {
                byte[] canvasIds = compoundTag.getByteArray(NBT_TAG_CANVAS_IDS);

                this.setCanvasIds(BitSet.valueOf(canvasIds));
            } else if (compoundTag.contains(NBT_TAG_CANVAS_LAST_ID)) {
                int lastCanvasId = compoundTag.getInt(NBT_TAG_CANVAS_LAST_ID);
                BitSet canvasIds = new BitSet(lastCanvasId + 1);
                canvasIds.flip(0, lastCanvasId + 1);

                this.setCanvasIds(canvasIds);
            } else {
                this.setCanvasIds(new BitSet());
            }

            this.setLastPaintingId(compoundTag.getInt(NBT_TAG_PAINTING_LAST_ID));
        }
    }
}
