package me.dantaeusb.zetter.network.packet;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.network.ServerHandler;
import me.dantaeusb.zetter.storage.AbstractCanvasData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class CCanvasHistoryPacket {
    public final int easelEntityId;
    public final UUID actionId;
    public final boolean canceled;

    public CCanvasHistoryPacket(int easelEntityId, UUID actionId, boolean canceled) {
        this.easelEntityId = easelEntityId;
        this.actionId = actionId;
        this.canceled = canceled;
    }

    /**
     * Reads the raw packet data from the data stream.
     * Seems like buffer is always at least 256 bytes, so we have to process written buffer size
     */
    public static CCanvasHistoryPacket readPacketData(FriendlyByteBuf buffer) {
        final int easelEntityId = buffer.readInt();
        final UUID actionId = buffer.readUUID();
        final boolean canceled = buffer.readBoolean();

        return new CCanvasHistoryPacket(easelEntityId, actionId, canceled);
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    public void writePacketData(FriendlyByteBuf buffer) {
        buffer.writeInt(this.easelEntityId);
        buffer.writeUUID(this.actionId);
        buffer.writeBoolean(this.canceled);
    }

    public static void handle(final CCanvasHistoryPacket packetIn, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.setPacketHandled(true);

        final ServerPlayer sendingPlayer = ctx.getSender();
        if (sendingPlayer == null) {
            Zetter.LOG.warn("EntityPlayerMP was null when CCanvasHistoryPacket was received");
        }

        ctx.enqueueWork(() -> ServerHandler.processCanvasHistory(packetIn, sendingPlayer));
    }
}