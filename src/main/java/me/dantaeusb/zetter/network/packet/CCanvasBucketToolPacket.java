package me.dantaeusb.zetter.network.packet;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.network.ServerHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public class CCanvasBucketToolPacket {
    public final int position;
    public final int color;

    public CCanvasBucketToolPacket(int position, int color) {
        this.position = position;
        this.color = color;
    }

    public static CCanvasBucketToolPacket readPacketData(FriendlyByteBuf buf) {
        final int position = buf.readInt();
        final int color = buf.readInt();

        return new CCanvasBucketToolPacket(position, color);
    }

    public void writePacketData(FriendlyByteBuf buf) {
        buf.writeInt(this.position);
        buf.writeInt(this.color);
    }

    public static void handle(final CCanvasBucketToolPacket packetIn, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        LogicalSide sideReceived = ctx.getDirection().getReceptionSide();
        ctx.setPacketHandled(true);

        if (sideReceived != LogicalSide.SERVER) {
            Zetter.LOG.warn("CCanvasBucketToolPacket received on wrong side:" + ctx.getDirection().getReceptionSide());
            return;
        }

        final ServerPlayer sendingPlayer = ctx.getSender();
        if (sendingPlayer == null) {
            Zetter.LOG.warn("EntityPlayerMP was null when CCanvasBucketToolPacket was received");
        }

        ctx.enqueueWork(() -> ServerHandler.processBucketTool(packetIn, sendingPlayer));
    }
}