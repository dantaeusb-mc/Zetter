package com.dantaeusb.zetter.network.packet;

import com.dantaeusb.zetter.Zetter;
import com.dantaeusb.zetter.network.ServerHandler;
import com.dantaeusb.zetter.storage.AbstractCanvasData;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class CCanvasBucketToolPacket {
    public final int position;
    public final int color;

    public CCanvasBucketToolPacket(int position, int color) {
        this.position = position;
        this.color = color;
    }

    public static CCanvasBucketToolPacket readPacketData(PacketBuffer buf) {
        final int position = buf.readInt();
        final int color = buf.readInt();

        return new CCanvasBucketToolPacket(position, color);
    }

    public void writePacketData(PacketBuffer buf) {
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

        final ServerPlayerEntity sendingPlayer = ctx.getSender();
        if (sendingPlayer == null) {
            Zetter.LOG.warn("EntityPlayerMP was null when CCanvasBucketToolPacket was received");
        }

        ctx.enqueueWork(() -> ServerHandler.processBucketTool(packetIn, sendingPlayer));
    }
}