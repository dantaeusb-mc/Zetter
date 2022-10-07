package me.dantaeusb.zetter.network.packet;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.menu.ArtistTableMenu;
import me.dantaeusb.zetter.network.ServerHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class CArtistTableModeChange {
    private final int windowId;
    private final ArtistTableMenu.Mode mode;

    public CArtistTableModeChange(int windowId, ArtistTableMenu.Mode mode) {
        this.windowId = windowId;
        this.mode = mode;
    }

    /**
     * Reads the raw packet data from the data stream.
     */
    public static CArtistTableModeChange readPacketData(FriendlyByteBuf buf) {
        int windowId = buf.readInt();
        byte modeId = buf.readByte();

        CArtistTableModeChange packet = new CArtistTableModeChange(windowId, ArtistTableMenu.Mode.getById(modeId));

        return packet;
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    public void writePacketData(FriendlyByteBuf buf) {
        buf.writeInt(this.windowId);
        buf.writeByte(this.mode.getId());
    }

    public int getWindowId() {
        return this.windowId;
    }

    public ArtistTableMenu.Mode getMode() {
        return this.mode;
    }

    public static void handle(final CArtistTableModeChange packetIn, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        LogicalSide sideReceived = ctx.getDirection().getReceptionSide();
        ctx.setPacketHandled(true);

        if (sideReceived != LogicalSide.SERVER) {
            Zetter.LOG.warn("ArtistTableModeChange received on wrong side:" + ctx.getDirection().getReceptionSide());
            return;
        }

        final ServerPlayer sendingPlayer = ctx.getSender();
        if (sendingPlayer == null) {
            Zetter.LOG.warn("EntityPlayerMP was null when ArtistTableModeChange was received");
        }

        ctx.enqueueWork(() -> ServerHandler.processArtistTableModeChange(packetIn, sendingPlayer));
    }
}