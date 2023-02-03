package me.dantaeusb.zetter.network.packet;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.network.ServerHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class CCanvasHistoryActionPacket {
    public final int easelEntityId;
    public final int actionId;
    public final boolean canceled;

    public CCanvasHistoryActionPacket(int easelEntityId, int actionId, boolean canceled) {
        this.easelEntityId = easelEntityId;
        this.actionId = actionId;
        this.canceled = canceled;
    }

    /**
     * Reads the raw packet data from the data stream.
     * Seems like buffer is always at least 256 bytes, so we have to process written buffer size
     */
    public static CCanvasHistoryActionPacket readPacketData(FriendlyByteBuf buffer) {
        final int easelEntityId = buffer.readInt();
        final int actionId = buffer.readInt();
        final boolean canceled = buffer.readBoolean();

        return new CCanvasHistoryActionPacket(easelEntityId, actionId, canceled);
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    public void writePacketData(FriendlyByteBuf buffer) {
        buffer.writeInt(this.easelEntityId);
        buffer.writeInt(this.actionId);
        buffer.writeBoolean(this.canceled);
    }

    public static void handle(final CCanvasHistoryActionPacket packetIn, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.setPacketHandled(true);

        final ServerPlayer sendingPlayer = ctx.getSender();
        if (sendingPlayer == null) {
            Zetter.LOG.warn("EntityPlayerMP was null when CCanvasHistoryPacket was received");
        }

        ctx.enqueueWork(() -> ServerHandler.processCanvasHistory(packetIn, sendingPlayer));
    }
}