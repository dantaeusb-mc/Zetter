package me.dantaeusb.zetter.network.packet;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.menu.painting.PaintingFrameBuffer;
import me.dantaeusb.zetter.network.ServerHandler;
import io.netty.buffer.ByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

/**
 * Painting update - get frame buffer from client when they're making changes
 * @todo: add entity and/or painting UUID
 */
public class CPaintingFrameBufferPacket {
    private PaintingFrameBuffer paintingFrameBuffer;

    public CPaintingFrameBufferPacket() {
    }

    public CPaintingFrameBufferPacket(PaintingFrameBuffer paintingFrameBuffer) {
        this.paintingFrameBuffer = paintingFrameBuffer;
    }

    /**
     * Reads the raw packet data from the data stream.
     * Seems like buf is always at least 256 bytes, so we have to process written buffer size
     */
    public static CPaintingFrameBufferPacket readPacketData(FriendlyByteBuf buf) {
        CPaintingFrameBufferPacket packet = new CPaintingFrameBufferPacket();

        long frameStartTime = buf.readLong();
        ByteBuf bufferData = buf.readBytes(buf.writerIndex() - buf.readerIndex());

        packet.paintingFrameBuffer = new PaintingFrameBuffer(frameStartTime, bufferData.nioBuffer());

        return packet;
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    public void writePacketData(FriendlyByteBuf buf) {
        buf.writeLong(this.paintingFrameBuffer.getFrameStartTime());
        buf.writeBytes(this.paintingFrameBuffer.getBufferData());
    }

    public PaintingFrameBuffer getFrameBuffer() {
        return this.paintingFrameBuffer;
    }

    public static void handle(final CPaintingFrameBufferPacket packetIn, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.setPacketHandled(true);

        final ServerPlayer sendingPlayer = ctx.getSender();
        if (sendingPlayer == null) {
            Zetter.LOG.warn("EntityPlayerMP was null when CPaintingUpdatePacket was received");
        }

        ctx.enqueueWork(() -> ServerHandler.processFrameBuffer(packetIn, sendingPlayer));
    }
}