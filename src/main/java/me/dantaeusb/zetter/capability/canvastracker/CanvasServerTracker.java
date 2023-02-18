package me.dantaeusb.zetter.capability.canvastracker;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.capability.paintingregistry.PaintingRegistry;
import me.dantaeusb.zetter.core.ZetterCanvasTypes;
import me.dantaeusb.zetter.core.ZetterNetwork;
import me.dantaeusb.zetter.core.ZetterRegistries;
import me.dantaeusb.zetter.event.CanvasRegisterEvent;
import me.dantaeusb.zetter.event.CanvasUnregisterEvent;
import me.dantaeusb.zetter.network.packet.SCanvasRemovalPacket;
import me.dantaeusb.zetter.network.packet.SCanvasSyncPacket;
import me.dantaeusb.zetter.storage.AbstractCanvasData;
import me.dantaeusb.zetter.storage.CanvasData;
import me.dantaeusb.zetter.storage.CanvasDataType;
import me.dantaeusb.zetter.storage.DummyCanvasData;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Direction;
import net.minecraft.util.SharedConstants;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fml.network.PacketDistributor;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.*;

public class CanvasServerTracker implements CanvasTracker {
    private static final String NBT_TAG_CANVAS_LAST_ID = "LastCanvasId";
    private static final String NBT_TAG_CANVAS_IDS = "CanvasIds";
    private static final String NBT_TAG_PAINTING_LAST_ID = "LastPaintingId";

    private ServerWorld level;

    protected BitSet canvasIds = new BitSet(1);
    protected int lastPaintingId = 0;

    private final Map<String, Vector<PlayerTrackingCanvas>> trackedCanvases = new HashMap<>();
    private final Vector<String> desyncCanvases = new Vector<>();
    private int ticksFromLastSync = 0;

    public CanvasServerTracker() {
        super();
    }

    public void setLevel(World level) {
        if (this.level != null) {
            throw new IllegalStateException("Cannot change level for capability");
        }

        if (!(level instanceof ServerWorld)) {
            throw new IllegalStateException("Only accepts ServerLevel");
        }

        this.level = (ServerWorld) level;
    }

    @Override
    public World getLevel() {
        return this.level;
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

    private void clearCanvasId(int id) {
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

    /**
     * @todo: [MED] Remove deprecated sections on next release
     * @todo: canvasCode null check
     * @param canvasCode
     * @return
     * @param <T>
     */
    @Override
    @Nullable
    @SuppressWarnings("unchecked")
    public <T extends AbstractCanvasData> T getCanvasData(String canvasCode) {
        assert this.level.getServer() != null;

        if (canvasCode == null) {
            return null;
        }

        boolean deprecatedType;
        int canvasTypeInt = -1;
        final Optional<? extends CanvasDataType<?>> type;

        // In versions before 0.18 we have to manually pre-read the file
        // To understand which type of canvas we are working with

        // File could be not created yet
        // Lol just re-set the value bruh
        // But will we have racing condition? Even if we do, we just keep all data
        AbstractCanvasData canvasData = this.level.getServer().overworld().getDataStorage().get(
                () -> ZetterCanvasTypes.DUMMY.get().supply(canvasCode),
                canvasCode
        );

        // If we had it in cache, type wil be different
        if (canvasData instanceof DummyCanvasData) {
            CompoundNBT compoundTag = ((DummyCanvasData) canvasData).getCacheCompoundTag();

            String canvasResourceLocation = compoundTag.getString(AbstractCanvasData.NBT_TAG_TYPE);

            if (canvasResourceLocation.isEmpty()) {
                if (!compoundTag.contains(AbstractCanvasData.NBT_TAG_TYPE_DEPRECATED)) {
                    throw new IllegalStateException("Cannot find canvas type");
                }

                canvasTypeInt = compoundTag.getInt(AbstractCanvasData.NBT_TAG_TYPE_DEPRECATED);

                switch (canvasTypeInt) {
                    case 1:
                        canvasResourceLocation = ZetterCanvasTypes.CANVAS.get().getRegistryName().toString();
                        break;
                    case 0:
                    case 2:
                    default:
                        canvasResourceLocation = ZetterCanvasTypes.PAINTING.get().getRegistryName().toString();
                        break;
                }
            }

            // Minor beta versions were saving data without modid separator
            deprecatedType = !canvasResourceLocation.contains(":");
            if (deprecatedType) {
                canvasResourceLocation = Zetter.MOD_ID + ":" + canvasResourceLocation;
            }

            final String finalCanvasResourceLocation = canvasResourceLocation;
            type = ZetterRegistries.CANVAS_TYPE.get().getEntries().stream()
                    .filter((entry) -> entry.getKey().location().toString().equals(finalCanvasResourceLocation))
                    .map(Map.Entry::getValue)
                    .findFirst();

            if (!type.isPresent()) {
                throw new IllegalStateException("No type of canvas " + canvasResourceLocation + " is registered");
            }

            canvasData = type.get().builder.supply(canvasCode);
            canvasData.load(compoundTag);

            this.level.getServer().overworld().getDataStorage().set(canvasData);

            // Remove deprecated tags
            if (canvasTypeInt != -1 || deprecatedType) {
                canvasData.setDirty();
            }
        }

        // Then we are supplying type we've read, and it loads its own data

        if (canvasData == null) {
            return null;
        }

        canvasData.correctData(this.level);

        return (T) canvasData;
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

        CanvasRegisterEvent.Pre preEvent = new CanvasRegisterEvent.Pre(canvasCode, canvasData, this.level, timestamp);
        MinecraftForge.EVENT_BUS.post(preEvent);

        this.level.getServer().overworld().getDataStorage().set(canvasData);

        CanvasRegisterEvent.Post postEvent = new CanvasRegisterEvent.Post(canvasCode, canvasData, this.level, timestamp);
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
        AbstractCanvasData canvasData = this.getCanvasData(canvasCode);

        if (canvasData == null) {
            Zetter.LOG.error("Cannot unregister non-existent canvas");
            return;
        }

        if (!canvasData.getType().equals(ZetterCanvasTypes.CANVAS.get())) {
            Zetter.LOG.error("Trying to unregister canvas of type " + canvasData.getType().getRegistryName().toString() + " on server side, not supported yet");
            return;
        }

        long timestamp = System.currentTimeMillis();

        CanvasUnregisterEvent.Pre preEvent = new CanvasUnregisterEvent.Pre(canvasCode, canvasData, this.level, timestamp);
        MinecraftForge.EVENT_BUS.post(preEvent);

        int canvasId = Integer.parseInt(canvasCode.substring(CanvasData.CODE_PREFIX.length()));
        this.clearCanvasId(canvasId);

        Vector<PlayerTrackingCanvas> trackingPlayers = this.trackedCanvases.get(canvasCode);

        if (trackingPlayers != null) {
            for (PlayerTrackingCanvas trackingPlayer : trackingPlayers) {
                SCanvasRemovalPacket canvasRemovalPacket = new SCanvasRemovalPacket(canvasCode, System.currentTimeMillis());

                ZetterNetwork.simpleChannel.send(PacketDistributor.PLAYER.with(
                    () -> (ServerPlayerEntity) this.level.getPlayerByUUID(trackingPlayer.playerId)),
                    canvasRemovalPacket
                );
            }
        }

        CanvasUnregisterEvent.Post postEvent = new CanvasUnregisterEvent.Post(canvasCode, canvasData, this.level, timestamp);
        MinecraftForge.EVENT_BUS.post(postEvent);
    }

    /**
     * Server handling - ticking, tracking from players and syncing
     */

    public void tick() {
        assert this.level.getServer() != null;
        this.ticksFromLastSync++;

        if (this.ticksFromLastSync < 20) {
            return;
        }

        /**
         * Send canvas sync message to every tracking player entity
         */
        MinecraftServer server = this.level.getServer();

        for (String canvasCode : this.desyncCanvases) {
            for (PlayerTrackingCanvas playerTrackingCanvas : this.getTrackingEntries(canvasCode)) {
                ServerPlayerEntity playerEntity = server.getPlayerList().getPlayer(playerTrackingCanvas.playerId);

                SCanvasSyncPacket<?> canvasSyncMessage = new SCanvasSyncPacket(canvasCode, this.getCanvasData(canvasCode), System.currentTimeMillis());
                ZetterNetwork.simpleChannel.send(PacketDistributor.PLAYER.with(() -> playerEntity), canvasSyncMessage);
            }
        }

        this.desyncCanvases.clear();
        this.ticksFromLastSync = 0;
    }

    /**
     * @todo: [MED] check if already tracking
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

    public CompoundNBT serializeNBT() {
        CompoundNBT compound = new CompoundNBT();

        compound.putByteArray(NBT_TAG_CANVAS_IDS, this.getCanvasIds().toByteArray());
        compound.putInt(NBT_TAG_CANVAS_LAST_ID, this.getLastCanvasId());
        compound.putInt(NBT_TAG_PAINTING_LAST_ID, this.getLastPaintingId());

        return compound;
    }

    public void deserializeNBT(INBT tag) {
        if (tag.getType() == CompoundNBT.TYPE) {
            CompoundNBT compoundTag = (CompoundNBT) tag;

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

    // Convert to/from NBT
    static class CanvasTrackerStorage implements Capability.IStorage<CanvasServerTracker> {
        @Override
        public INBT writeNBT(Capability<CanvasServerTracker> capability, CanvasServerTracker instance, @Nullable Direction side) {
            return instance.serializeNBT();
        }

        @Override
        public void readNBT(Capability<CanvasServerTracker> capability, CanvasServerTracker instance, Direction side, @Nullable INBT nbt) {
            instance.deserializeNBT(nbt);
        }
    }
}