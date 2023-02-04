package me.dantaeusb.zetter.network.packet;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.entity.item.state.representation.CanvasAction;
import me.dantaeusb.zetter.entity.item.state.representation.CanvasSnapshot;
import me.dantaeusb.zetter.network.ClientHandler;
import net.minecraft.network.PacketBuffer;
import net.minecraft.world.World;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.LogicalSidedProvider;
import net.minecraftforge.fml.network.NetworkEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Send snapshot of a canvas and actions to keep every
 * using player history of changes up to date
 */
public class SEaselStateSyncPacket {
    public static final int MAX_ACTIONS = 50;

    public final int easelEntityId;
    public final String canvasCode;

    /* If at the moment of creation of this packet,
     * all sent data (actions or/and snapshots) are
     * the latest available data for the easel state,
     * so we can consider our easels sync after processing
     * those; or if we expect more large updates to come
     */
    public final boolean sync;

    public final @Nullable CanvasSnapshot snapshot;
    public final @Nullable ArrayList<CanvasAction> unsyncedActions;

    public SEaselStateSyncPacket(int easelEntityId, String canvasCode, boolean sync, @Nullable CanvasSnapshot snapshot, @Nullable ArrayList<CanvasAction> unsyncedActions) {
        this.easelEntityId = easelEntityId;
        this.canvasCode = canvasCode;
        this.sync = sync;

        this.snapshot = snapshot;

        this.unsyncedActions = unsyncedActions;

        if (snapshot == null && (unsyncedActions == null || unsyncedActions.isEmpty())) {
            Zetter.LOG.error("Preparing empty easel sync packet");
        }
    }

    /**
     * Reads the raw packet data from the data stream.
     */
    public static SEaselStateSyncPacket readPacketData(PacketBuffer networkBuffer) {
        try {
            final int easelEntityId = networkBuffer.readInt();
            final String canvasCode = networkBuffer.readUtf(128);
            final boolean sync = networkBuffer.readBoolean();

            CanvasSnapshot snapshot = null;
            final boolean hasSnapshot = networkBuffer.readBoolean();

            if (hasSnapshot) {
                final int snapshotId = networkBuffer.readInt();
                final int snapshotColorLength = networkBuffer.readInt();
                final byte[] snapshotColor = networkBuffer.readByteArray(snapshotColorLength);
                final long snapshotTimestamp = networkBuffer.readLong();

                snapshot = CanvasSnapshot.createNetworkSnapshot(snapshotId, snapshotColor, snapshotTimestamp);
            }

            int actionBuffersCount = networkBuffer.readInt();

            if (actionBuffersCount == 0) {
                return new SEaselStateSyncPacket(easelEntityId, canvasCode, sync, snapshot, null);
            }

            ArrayList<CanvasAction> unsyncedActions = new ArrayList<>();

            for (int i = 0; i < actionBuffersCount; i++) {
                @Nullable CanvasAction action = CanvasAction.readPacketData(networkBuffer);

                if (action != null) {
                    unsyncedActions.add(action);
                } else {
                    Zetter.LOG.error("Cannot retrieve actions from buffer");
                }
            }

            return new SEaselStateSyncPacket(easelEntityId, canvasCode, sync, snapshot, unsyncedActions);
        } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
            Zetter.LOG.warn("Exception while reading SEaselStateSync: " + e);
            return null;
        }
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    public void writePacketData(PacketBuffer networkBuffer) {
        networkBuffer.writeInt(this.easelEntityId);
        networkBuffer.writeUtf(this.canvasCode, 128);
        networkBuffer.writeBoolean(this.sync);

        networkBuffer.writeBoolean(this.snapshot != null);

        if (this.snapshot != null) {
            networkBuffer.writeInt(this.snapshot.id);
            networkBuffer.writeInt(this.snapshot.colors.length);
            networkBuffer.writeByteArray(this.snapshot.colors);
            networkBuffer.writeLong(this.snapshot.timestamp);
        }

        if (this.unsyncedActions != null && !this.unsyncedActions.isEmpty()) {
            networkBuffer.writeInt(this.unsyncedActions.size());

            for (CanvasAction actionBuffer : this.unsyncedActions) {
                CanvasAction.writePacketData(actionBuffer, networkBuffer);
            }
        } else {
            networkBuffer.writeInt(0);
        }
    }

    public static void handle(final SEaselStateSyncPacket packetIn, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        LogicalSide sideReceived = ctx.getDirection().getReceptionSide();
        ctx.setPacketHandled(true);

        Optional<World> clientWorld = LogicalSidedProvider.CLIENTWORLD.get(sideReceived);
        if (!clientWorld.isPresent()) {
            Zetter.LOG.warn("SEaselStateSync context could not provide a ClientWorld.");
            return;
        }

        ctx.enqueueWork(() -> ClientHandler.processEaselStateSync(packetIn, clientWorld.get()));
    }

    @Override
    public String toString()
    {
        return "SEaselStateSync[easel=" + this.easelEntityId + "painting=" + this.canvasCode + "]";
    }
}