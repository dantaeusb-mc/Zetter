package me.dantaeusb.zetter.network.packet;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.core.ZetterRegistries;
import me.dantaeusb.zetter.network.ClientHandler;
import me.dantaeusb.zetter.storage.AbstractCanvasData;
import me.dantaeusb.zetter.storage.CanvasDataType;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.Hand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.LogicalSidedProvider;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.Optional;
import java.util.function.Supplier;

public class SCanvasSyncViewPacket<T extends AbstractCanvasData> extends SCanvasSyncPacket<T> {
    private final Hand hand;

    public SCanvasSyncViewPacket(String canvasCode, T canvasData, long timestamp, Hand hand) {
        super(canvasCode, canvasData, timestamp);

        this.hand = hand;
    }

    /**
     * Reads the raw packet data from the data stream.
     */
    public static SCanvasSyncViewPacket<?> readPacketData(PacketBuffer networkBuffer) {
        try {
            String type = networkBuffer.readUtf(128);
            String canvasCode = networkBuffer.readUtf(128);
            long timestamp = networkBuffer.readLong();
            byte handCode = networkBuffer.readByte();
            Hand hand = Hand.values()[handCode];

            CanvasDataType<?> canvasDataType = ZetterRegistries.CANVAS_TYPE.get().getValue(new ResourceLocation(type));

            if (canvasDataType == null) {
                throw new IllegalArgumentException("Unable to find canvas type " + type);
            }

            AbstractCanvasData canvasData = canvasDataType.readPacketData(networkBuffer);

            return new SCanvasSyncViewPacket(canvasCode, canvasData, timestamp, hand);
        } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
            Zetter.LOG.warn("Exception while reading SCanvasSyncMessage: " + e);
            return null;
        }
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    public void writePacketData(PacketBuffer networkBuffer) {
        networkBuffer.writeUtf(this.canvasData.getType().getRegistryName().toString(), 128);
        networkBuffer.writeUtf(this.canvasCode, 128);
        networkBuffer.writeLong(this.timestamp);
        networkBuffer.writeByte(this.hand.ordinal());

        CanvasDataType<T> canvasDataType = (CanvasDataType<T>) ZetterRegistries.CANVAS_TYPE.get().getValue(this.canvasData.getType().getRegistryName());

        assert canvasDataType != null;
        canvasDataType.writePacketData(this.canvasCode, this.canvasData, networkBuffer);

    }

    public Hand getHand() {
        return this.hand;
    }

    public static void handle(final SCanvasSyncViewPacket<?> packetIn, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        LogicalSide sideReceived = ctx.getDirection().getReceptionSide();
        ctx.setPacketHandled(true);

        Optional<World> clientWorld = LogicalSidedProvider.CLIENTWORLD.get(sideReceived);
        if (!clientWorld.isPresent()) {
            Zetter.LOG.warn("SCanvasSyncViewMessage context could not provide a ClientWorld.");
            return;
        }

        ctx.enqueueWork(() -> ClientHandler.processCanvasSyncView(packetIn, clientWorld.get()));
    }

    @Override
    public String toString()
    {
        return "SCanvasSyncViewMessage[canvas=" + this.canvasCode + ",timestamp=" + this.timestamp + "]";
    }
}