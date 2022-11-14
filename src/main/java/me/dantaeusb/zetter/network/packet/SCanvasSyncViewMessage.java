package me.dantaeusb.zetter.network.packet;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.core.ZetterRegistries;
import me.dantaeusb.zetter.network.ClientHandler;
import me.dantaeusb.zetter.storage.AbstractCanvasData;
import me.dantaeusb.zetter.storage.CanvasDataType;
import me.dantaeusb.zetter.storage.PaintingData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.LogicalSidedProvider;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;

import java.util.Optional;
import java.util.function.Supplier;

public class SCanvasSyncViewMessage<T extends AbstractCanvasData> extends SCanvasSyncMessage<T> {
    private final InteractionHand hand;

    public SCanvasSyncViewMessage(String canvasCode, T canvasData, long timestamp, InteractionHand hand) {
        super(canvasCode, canvasData, timestamp);

        this.hand = hand;
    }

    /**
     * Reads the raw packet data from the data stream.
     */
    public static SCanvasSyncViewMessage<?> readPacketData(FriendlyByteBuf networkBuffer) {
        try {
            String type = networkBuffer.readUtf(128);
            String canvasCode = networkBuffer.readUtf(128);
            long timestamp = networkBuffer.readLong();
            byte handCode = networkBuffer.readByte();
            InteractionHand hand = InteractionHand.values()[handCode];

            CanvasDataType<?> canvasDataType = ZetterRegistries.CANVAS_TYPE.get().getValue(new ResourceLocation(type));

            if (canvasDataType == null) {
                throw new IllegalArgumentException("Unable to find canvas type " + type);
            }

            AbstractCanvasData canvasData = canvasDataType.readPacketData(networkBuffer);

            return new SCanvasSyncViewMessage(canvasCode, canvasData, timestamp, hand);
        } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
            Zetter.LOG.warn("Exception while reading SCanvasSyncMessage: " + e);
            return null;
        }
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    public void writePacketData(FriendlyByteBuf networkBuffer) {
        networkBuffer.writeUtf(this.canvasData.resourceLocation.toString(), 128);
        networkBuffer.writeUtf(this.canvasCode, 128);
        networkBuffer.writeLong(this.timestamp);
        networkBuffer.writeByte(this.hand.ordinal());

        CanvasDataType<T> canvasDataType = (CanvasDataType<T>) ZetterRegistries.CANVAS_TYPE.get().getValue(this.canvasData.resourceLocation);

        assert canvasDataType != null;
        canvasDataType.writePacketData(this.canvasData, networkBuffer);

    }

    public InteractionHand getHand() {
        return this.hand;
    }

    public static void handle(final SCanvasSyncViewMessage<?> packetIn, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        LogicalSide sideReceived = ctx.getDirection().getReceptionSide();
        ctx.setPacketHandled(true);

        Optional<Level> clientWorld = LogicalSidedProvider.CLIENTWORLD.get(sideReceived);
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