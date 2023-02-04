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

public record SCanvasRemovalPacket(String canvasCode, long timestamp) {
    /**
     * Reads the raw packet data from the data stream.
     */
    public static SCanvasRemovalPacket readPacketData(PacketBuffer networkBuffer) {
        try {
            String canvasCode = networkBuffer.readUtf(128);
            long timestamp = networkBuffer.readLong();

            return new SCanvasRemovalPacket(canvasCode, timestamp);
        } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
            Zetter.LOG.warn("Exception while reading SCanvasSyncMessage: " + e);
            return null;
        }
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    public void writePacketData(PacketBuffer networkBuffer) {
        networkBuffer.writeUtf(this.canvasCode, 128);
        networkBuffer.writeLong(this.timestamp);
    }

    public static void handle(final SCanvasRemovalPacket packetIn, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        LogicalSide sideReceived = ctx.getDirection().getReceptionSide();
        ctx.setPacketHandled(true);

        Optional<World> clientWorld = LogicalSidedProvider.CLIENTWORLD.get(sideReceived);
        if (clientWorld.isEmpty()) {
            Zetter.LOG.error("SCanvasRemovalMessage context could not provide a ClientWorld.");
            return;
        }

        ctx.enqueueWork(() -> ClientHandler.processCanvasRemoval(packetIn, clientWorld.get()));
    }

    @Override
    public String toString() {
        return "SCanvasRemovalMessage[canvas=" + this.canvasCode + ",timestamp=" + this.timestamp + "]";
    }
}