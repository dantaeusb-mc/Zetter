package me.dantaeusb.zetter.network.packet;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.network.ClientHandler;
import me.dantaeusb.zetter.storage.AbstractCanvasData;
import me.dantaeusb.zetter.storage.PaintingData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.LogicalSidedProvider;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;

import java.util.Optional;
import java.util.function.Supplier;

public class SCanvasSyncViewMessage extends SCanvasSyncMessage {
    private final InteractionHand hand;

    public SCanvasSyncViewMessage(String canvasCode, AbstractCanvasData canvasData, long timestamp, InteractionHand hand) {
        super(canvasCode, canvasData, timestamp);

        this.hand = hand;
    }

    /**
     * Reads the raw packet data from the data stream.
     */
    public static SCanvasSyncViewMessage readPacketData(FriendlyByteBuf networkBuffer) {
        try {
            String canvasCode = networkBuffer.readUtf();
            long timestamp = networkBuffer.readLong();
            byte typeId = networkBuffer.readByte();

            AbstractCanvasData.Type type = AbstractCanvasData.Type.getTypeById(typeId);
            AbstractCanvasData readCanvasData = CanvasContainer.readPacketCanvasData(networkBuffer);

            byte handCode = networkBuffer.readByte();
            InteractionHand hand = InteractionHand.values()[handCode];

            if (type == AbstractCanvasData.Type.PAINTING) {
                String paintingName = networkBuffer.readUtf();
                String authorName = networkBuffer.readUtf();

                ((PaintingData) readCanvasData).setMetaProperties(authorName, paintingName);
            }

            return new SCanvasSyncViewMessage(canvasCode, readCanvasData, timestamp, hand);
        } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
            Zetter.LOG.warn("Exception while reading SCanvasSyncMessage: " + e);
            return null;
        }
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    public void writePacketData(FriendlyByteBuf networkBuffer) {
        networkBuffer.writeUtf(this.canvasCode);
        networkBuffer.writeLong(this.timestamp);
        networkBuffer.writeByte(this.type.getId());
        CanvasContainer.writePacketCanvasData(networkBuffer, this.canvasData);
        networkBuffer.writeByte(this.hand.ordinal());

        if (this.canvasData instanceof PaintingData) {
            // @todo: [LOW] Proper length based on game limitations
            networkBuffer.writeUtf(((PaintingData) this.canvasData).getPaintingTitle(), 64);
            networkBuffer.writeUtf(((PaintingData) this.canvasData).getAuthorName(), 64);
        }
    }

    public InteractionHand getHand() {
        return this.hand;
    }

    public static void handle(final SCanvasSyncViewMessage packetIn, Supplier<NetworkEvent.Context> ctxSupplier) {
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