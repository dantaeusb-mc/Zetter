package me.dantaeusb.zetter.network.packet;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.menu.painting.PaintingActionBuffer;
import me.dantaeusb.zetter.network.ServerHandler;
import io.netty.buffer.ByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.function.Supplier;

/**
 * Painting update - get frame buffer from client when they're making changes
 */
public class CCanvasActionBufferPacket {
    private Queue<PaintingActionBuffer> paintingActionBuffers;

    public CCanvasActionBufferPacket() {
        this.paintingActionBuffers = new LinkedList<>();
    }

    public CCanvasActionBufferPacket(Queue<PaintingActionBuffer> paintingActionBuffers) {
        this.paintingActionBuffers = paintingActionBuffers;
    }

    /**
     * Reads the raw packet data from the data stream.
     * Seems like buffer is always at least 256 bytes, so we have to process written buffer size
     */
    public static CCanvasActionBufferPacket readPacketData(FriendlyByteBuf buffer) {
        int actionBuffersCount = buffer.readInt();

        CCanvasActionBufferPacket packet = new CCanvasActionBufferPacket();

        for (int i = 0; i < actionBuffersCount; i++) {
            packet.paintingActionBuffers.add(PaintingActionBuffer.readPacketData(buffer));
        }

        return packet;
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    public void writePacketData(FriendlyByteBuf buffer) {
        buffer.writeInt(this.paintingActionBuffers.size());

        for (PaintingActionBuffer actionBuffer : this.paintingActionBuffers) {
            PaintingActionBuffer.writePacketData(actionBuffer, buffer);
        }
    }

    public static void handle(final CCanvasActionBufferPacket packetIn, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.setPacketHandled(true);

        final ServerPlayer sendingPlayer = ctx.getSender();
        if (sendingPlayer == null) {
            Zetter.LOG.warn("EntityPlayerMP was null when CPaintingUpdatePacket was received");
        }

        ctx.enqueueWork(() -> ServerHandler.processFrameBuffer(packetIn, sendingPlayer));
    }
}