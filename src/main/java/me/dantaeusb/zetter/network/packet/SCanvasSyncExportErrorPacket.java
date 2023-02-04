package me.dantaeusb.zetter.network.packet;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.network.ClientHandler;
import net.minecraft.network.PacketBuffer;
import net.minecraft.world.World;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.LogicalSidedProvider;
import net.minecraftforge.fml.network.NetworkEvent;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.Supplier;

public class SCanvasSyncExportErrorPacket {
    public final String errorCode;
    public final String errorMessage;

    public SCanvasSyncExportErrorPacket(String errorCode, @Nullable String errorMessage) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    /**
     * Reads the raw packet data from the data stream.
     */
    public static SCanvasSyncExportErrorPacket readPacketData(PacketBuffer networkBuffer) {
        try {
            String errorCode = networkBuffer.readUtf(32767);
            String errorMessage = null;
            if (networkBuffer.readBoolean()) {
                errorMessage = networkBuffer.readUtf(32767);
            }

            return new SCanvasSyncExportErrorPacket(errorCode, errorMessage);
        } catch (Exception e) {
            Zetter.LOG.warn("Exception while reading SCanvasSyncExportErrorPacket: " + e);
            return null;
        }
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    public void writePacketData(PacketBuffer networkBuffer) {
        networkBuffer.writeUtf(this.errorCode, 32767);
        networkBuffer.writeBoolean(this.errorMessage != null);

        if (this.errorMessage != null) {
            networkBuffer.writeUtf(this.errorMessage, 32767);
        }
    }

    public static void handle(final SCanvasSyncExportErrorPacket packetIn, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        LogicalSide sideReceived = ctx.getDirection().getReceptionSide();
        ctx.setPacketHandled(true);

        Optional<World> clientWorld = LogicalSidedProvider.CLIENTWORLD.get(sideReceived);
        if (!clientWorld.isPresent()) {
            Zetter.LOG.warn("SCanvasSyncExportErrorPacket context could not provide a ClientWorld.");
            return;
        }

        ctx.enqueueWork(() -> ClientHandler.processCanvasSyncExportError(packetIn, clientWorld.get()));
    }

    @Override
    public String toString()
    {
        return "SCanvasSyncViewMessage[errorCode=" + this.errorCode + "]";
    }
}