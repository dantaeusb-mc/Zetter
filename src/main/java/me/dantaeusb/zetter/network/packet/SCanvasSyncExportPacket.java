package me.dantaeusb.zetter.network.packet;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.core.ZetterCanvasTypes;
import me.dantaeusb.zetter.network.ClientHandler;
import me.dantaeusb.zetter.storage.PaintingData;
import net.minecraft.network.PacketBuffer;
import net.minecraft.world.World;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.LogicalSidedProvider;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.Optional;
import java.util.function.Supplier;

public class SCanvasSyncExportPacket extends SCanvasSyncPacket<PaintingData> {
    public SCanvasSyncExportPacket(String canvasCode, PaintingData paintingData, long timestamp) {
        super(canvasCode, paintingData, timestamp);
    }

    /**
     * Reads the raw packet data from the data stream.
     */
    public static SCanvasSyncExportPacket readPacketData(PacketBuffer networkBuffer) {
        try {
            String canvasCode = networkBuffer.readUtf(128);
            long timestamp = networkBuffer.readLong();

            PaintingData canvasData = ZetterCanvasTypes.PAINTING.get().readPacketData(networkBuffer);

            return new SCanvasSyncExportPacket(canvasCode, canvasData, timestamp);
        } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
            Zetter.LOG.warn("Exception while reading SCanvasSyncExportPacket: " + e);
            return null;
        }
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    public void writePacketData(PacketBuffer networkBuffer) {
        networkBuffer.writeUtf(this.canvasCode, 128);
        networkBuffer.writeLong(this.timestamp);

        ZetterCanvasTypes.PAINTING.get().writePacketData(this.canvasData, networkBuffer);
    }

    public static void handle(final SCanvasSyncExportPacket packetIn, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        LogicalSide sideReceived = ctx.getDirection().getReceptionSide();
        ctx.setPacketHandled(true);

        Optional<World> clientWorld = LogicalSidedProvider.CLIENTWORLD.get(sideReceived);
        if (!clientWorld.isPresent()) {
            Zetter.LOG.warn("SCanvasSyncExportPacket context could not provide a ClientWorld.");
            return;
        }

        ctx.enqueueWork(() -> ClientHandler.processCanvasSyncExportError(packetIn, clientWorld.get()));
    }

    @Override
    public String toString()
    {
        return "SCanvasSyncViewMessage[canvas=" + this.canvasCode + ",timestamp=" + this.timestamp + "]";
    }
}