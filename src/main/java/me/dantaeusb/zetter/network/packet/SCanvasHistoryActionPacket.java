package me.dantaeusb.zetter.network.packet;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.network.ClientHandler;
import net.minecraft.network.PacketBuffer;
import net.minecraft.world.World;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.LogicalSidedProvider;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Needed when several players are editing, to notify other players
 * that one of them canceled an action
 */
public class SCanvasHistoryActionPacket {
    public final int easelEntityId;
    public final int actionId;
    public final boolean canceled;

    public SCanvasHistoryActionPacket(int easelEntityId, int actionId, boolean canceled) {
        this.easelEntityId = easelEntityId;
        this.actionId = actionId;
        this.canceled = canceled;
    }

    /**
     * Reads the raw packet data from the data stream.
     * Seems like buffer is always at least 256 bytes, so we have to process written buffer size
     */
    public static SCanvasHistoryActionPacket readPacketData(PacketBuffer buffer) {
        final int easelEntityId = buffer.readInt();
        final int actionId = buffer.readInt();
        final boolean canceled = buffer.readBoolean();

        return new SCanvasHistoryActionPacket(easelEntityId, actionId, canceled);
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    public void writePacketData(PacketBuffer buffer) {
        buffer.writeInt(this.easelEntityId);
        buffer.writeInt(this.actionId);
        buffer.writeBoolean(this.canceled);
    }

    public static void handle(final SCanvasHistoryActionPacket packetIn, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        LogicalSide sideReceived = ctx.getDirection().getReceptionSide();
        ctx.setPacketHandled(true);

        Optional<World> clientWorld = LogicalSidedProvider.CLIENTWORLD.get(sideReceived);
        if (!clientWorld.isPresent()) {
            Zetter.LOG.warn("SCanvasHistoryActionPacket context could not provide a ClientWorld.");
            return;
        }

        ctx.enqueueWork(() -> ClientHandler.processCanvasHistory(packetIn, clientWorld.get()));
    }
}