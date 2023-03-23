package me.dantaeusb.zetter.network.packet;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.network.ServerHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class CPaletteUpdatePacket {
    private int slotIndex;
    private int color;

    public CPaletteUpdatePacket() {
    }

    public CPaletteUpdatePacket(int slotIndex, int color) {
        this.slotIndex = slotIndex;
        this.color = color;
    }

    /**
     * Reads the raw packet data from the data stream.
     */
    public static CPaletteUpdatePacket readPacketData(FriendlyByteBuf buf) {
        CPaletteUpdatePacket packet = new CPaletteUpdatePacket();

        packet.slotIndex = buf.readInt();
        packet.color = buf.readInt();

        return packet;
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    public void writePacketData(FriendlyByteBuf buf) {
        buf.writeInt(this.slotIndex);
        buf.writeInt(this.color);
    }

    public int getSlotIndex() {
        return this.slotIndex;
    }

    public int getColor() {
        return this.color;
    }

    public static void handle(final CPaletteUpdatePacket packetIn, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        LogicalSide sideReceived = ctx.getDirection().getReceptionSide();
        ctx.setPacketHandled(true);

        if (sideReceived != LogicalSide.SERVER) {
            Zetter.LOG.warn("PaletteUpdatePacket received on wrong side:" + ctx.getDirection().getReceptionSide());
            return;
        }

        final ServerPlayer sendingPlayer = ctx.getSender();
        if (sendingPlayer == null) {
            Zetter.LOG.warn("EntityPlayerMP was null when PaletteUpdatePacket was received");
        }

        ctx.enqueueWork(() -> ServerHandler.processPaletteUpdate(packetIn, sendingPlayer));
    }
}