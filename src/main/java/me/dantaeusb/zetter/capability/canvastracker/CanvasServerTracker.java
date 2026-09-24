package me.dantaeusb.zetter.capability.canvastracker;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.core.ZetterCanvasTypes;
import me.dantaeusb.zetter.core.ZetterNetwork;
import me.dantaeusb.zetter.core.ZetterRegistries;
import me.dantaeusb.zetter.event.CanvasRegisterEvent;
import me.dantaeusb.zetter.event.CanvasUnregisterEvent;
import me.dantaeusb.zetter.network.packet.SCanvasRemovalPacket;
import me.dantaeusb.zetter.network.packet.SCanvasSyncPacket;
import me.dantaeusb.zetter.storage.AbstractCanvasData;
import me.dantaeusb.zetter.storage.CanvasData;
import me.dantaeusb.zetter.storage.DrawingData;
import me.dantaeusb.zetter.storage.CanvasDataType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.*;

public class CanvasServerTracker implements CanvasTracker {
    private ServerLevel level;

    protected BitSet canvasIds = new BitSet(1);
    protected int lastPaintingId = 0;

    /**
     * Virtual canvases - those that are not saved in world storage
     * and are kept in memory only for short time (i.e. during
     * stitching/sewing process)
     */
    private final Map<String, AbstractCanvasData> virtualCanvases = new HashMap<>();
    private final Map<String, Vector<PlayerTrackingCanvas>> trackedCanvases = new HashMap<>();

    /**
     * Canvases changed since the last sync, mapped to the players that do not need
     * it: those are painting on the canvas and get the change as an action instead
     */
    private final Map<String, Set<UUID>> desyncCanvases = new HashMap<>();
    private int ticksFromLastSync = 0;

    public CanvasServerTracker() {
        super();
    }

    @Override
    public void setLevel(Level level) {
        if (this.level != null) {
            throw new IllegalStateException("Cannot change level for capability");
        }

        if (!(level instanceof ServerLevel)) {
            throw new IllegalStateException("Only accepts ServerLevel");
        }

        this.level = (ServerLevel) level;
    }

    @Override
    public Level getLevel() {
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
        this.markCanvasDesync(canvasCode, Collections.emptySet());
    }

    /**
     * @param canvasCode
     * @param playersReceivingActions players that follow this canvas action by action
     */
    public void markCanvasDesync(String canvasCode, Collection<UUID> playersReceivingActions) {
        this.desyncCanvases.computeIfAbsent(canvasCode, code -> new HashSet<>()).addAll(playersReceivingActions);
    }

    /**
     * @todo: [MED] Remove deprecated sections on next release
     * @param canvasCode
     * @return
     * @param <T>
     */
    @Override
    @Nullable
    @SuppressWarnings("unchecked")
    public <T extends AbstractCanvasData> T getCanvasData(String canvasCode) {
        if (canvasCode == null) {
            return null;
        }

        if (this.virtualCanvases.containsKey(canvasCode)) {
            return (T) this.virtualCanvases.get(canvasCode);
        }

        return this.level.getServer().overworld().getDataStorage().get(
            (compoundTag) -> {
                int canvasTypeInt = -1;
                String canvasResourceLocation = compoundTag.getString(AbstractCanvasData.NBT_TAG_TYPE);

                if (canvasResourceLocation.isEmpty()) {
                    if (!compoundTag.contains(AbstractCanvasData.NBT_TAG_TYPE_DEPRECATED)) {
                        throw new IllegalStateException("Cannot find canvas type");
                    }

                    canvasTypeInt = compoundTag.getInt(AbstractCanvasData.NBT_TAG_TYPE_DEPRECATED);

                    switch (canvasTypeInt) {
                        case 1:
                            canvasResourceLocation = ZetterCanvasTypes.CANVAS.get().resourceLocation.toString();
                            break;
                        case 0:
                        case 2:
                        default:
                            canvasResourceLocation = ZetterCanvasTypes.PAINTING.get().resourceLocation.toString();
                            break;
                    }
                }

                final String finalCanvasResourceLocation = canvasResourceLocation;
                Optional<? extends CanvasDataType<?>> type = ZetterRegistries.CANVAS_TYPE.get().getEntries().stream()
                    .filter((entry) -> entry.getKey().location().toString().equals(finalCanvasResourceLocation))
                    .map(Map.Entry::getValue)
                    .findFirst();

                if (type.isEmpty()) {
                    throw new IllegalStateException("No type of canvas " + canvasResourceLocation + " is registered");
                }

                T canvasData = (T) type.get().loadFromNbt(compoundTag);
                canvasData.correctData(this.level);

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

        CanvasRegisterEvent.Pre preEvent = new CanvasRegisterEvent.Pre(canvasCode, canvasData, this.level, timestamp);
        MinecraftForge.EVENT_BUS.post(preEvent);

        this.level.getServer().overworld().getDataStorage().set(canvasCode, canvasData);

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

        if (!canvasData.getType().equals(ZetterCanvasTypes.CANVAS.get())
            && !canvasData.getType().equals(ZetterCanvasTypes.DRAWING.get())) {
            Zetter.LOG.error("Trying to unregister canvas of type " + canvasData.getType().resourceLocation.toString() + " on server side, not supported yet");
            return;
        }

        long timestamp = System.currentTimeMillis();

        CanvasUnregisterEvent.Pre preEvent = new CanvasUnregisterEvent.Pre(canvasCode, canvasData, this.level, timestamp);
        MinecraftForge.EVENT_BUS.post(preEvent);

        if (canvasCode.startsWith(CanvasData.CODE_PREFIX)) {
            int canvasId = Integer.parseInt(canvasCode.substring(CanvasData.CODE_PREFIX.length()));
            this.clearCanvasId(canvasId);
        }

        this.blankCanvasData(canvasCode, canvasData);

        Vector<PlayerTrackingCanvas> trackingPlayers = this.trackedCanvases.get(canvasCode);

        if (trackingPlayers != null) {
            for (PlayerTrackingCanvas trackingPlayer : trackingPlayers) {
                SCanvasRemovalPacket canvasRemovalPacket = new SCanvasRemovalPacket(canvasCode, System.currentTimeMillis());

                ZetterNetwork.simpleChannel.send(PacketDistributor.PLAYER.with(
                    () -> (ServerPlayer) this.level.getPlayerByUUID(trackingPlayer.playerId)),
                    canvasRemovalPacket
                );
            }
        }

        CanvasUnregisterEvent.Post postEvent = new CanvasUnregisterEvent.Post(canvasCode, canvasData, this.level, timestamp);
        MinecraftForge.EVENT_BUS.post(postEvent);
    }

    /**
     * Wipe what an unregistered canvas was holding.
     *
     * The world's storage has no way to drop an entry, so the entry is replaced with the smallest blank
     * canvas there is
     *
     * @param canvasCode
     * @param canvasData the data being unregistered, which decides what kind of
     *                   blank replaces it
     */
    private void blankCanvasData(String canvasCode, AbstractCanvasData canvasData) {
        final int side = AbstractCanvasData.Resolution.x16.getNumeric();

        final AbstractCanvasData blank = canvasData.getType().builder.createFresh(
            AbstractCanvasData.Resolution.x16, side, side, 0x00000000
        );

        blank.setDirty();

        this.level.getServer().overworld().getDataStorage().set(canvasCode, blank);
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
        MinecraftServer server = this.level.getServer();

        for (Map.Entry<String, Set<UUID>> desyncEntry : this.desyncCanvases.entrySet()) {
            final String canvasCode = desyncEntry.getKey();
            final Vector<PlayerTrackingCanvas> trackingEntries = this.getTrackingEntries(canvasCode);

            if (trackingEntries.isEmpty()) {
                continue;
            }

            final AbstractCanvasData canvasData = this.getCanvasData(canvasCode);

            if (canvasData == null) {
                Zetter.LOG.warn("Unable to sync unknown canvas " + canvasCode);
                continue;
            }

            final SCanvasSyncPacket<?> canvasSyncMessage = new SCanvasSyncPacket(canvasCode, canvasData, System.currentTimeMillis());

            for (PlayerTrackingCanvas playerTrackingCanvas : trackingEntries) {
                // Already up to date from the easel they are painting on
                if (desyncEntry.getValue().contains(playerTrackingCanvas.playerId)) {
                    continue;
                }

                final ServerPlayer playerEntity = server.getPlayerList().getPlayer(playerTrackingCanvas.playerId);

                // Logged off since they started tracking
                if (playerEntity == null) {
                    continue;
                }

                ZetterNetwork.simpleChannel.send(PacketDistributor.PLAYER.with(() -> playerEntity), canvasSyncMessage);
            }
        }

        this.desyncCanvases.clear();
        this.ticksFromLastSync = 0;
    }

    /**
     * @param playerId
     * @param canvasName
     */
    public void trackCanvas(UUID playerId, String canvasName) {
        Vector<PlayerTrackingCanvas> trackingEntries = this.getTrackingEntries(canvasName);

        for (PlayerTrackingCanvas playerTrackingCanvas : trackingEntries) {
            if (playerTrackingCanvas.playerId.equals(playerId)) {
                return;
            }
        }

        trackingEntries.add(new PlayerTrackingCanvas(playerId, canvasName));
    }

    public void stopTrackingCanvas(UUID playerId, String canvasName) {
        if (!this.trackedCanvases.containsKey(canvasName)) {
            return;
        }

        Vector<PlayerTrackingCanvas> trackingEntries = this.trackedCanvases.get(canvasName);
        trackingEntries.removeIf((PlayerTrackingCanvas entry) -> entry.playerId.equals(playerId));
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
}