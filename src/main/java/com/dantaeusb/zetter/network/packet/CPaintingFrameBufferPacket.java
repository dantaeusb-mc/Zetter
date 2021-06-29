package com.dantaeusb.zetter.network.packet;

import com.dantaeusb.zetter.Zetter;
import com.dantaeusb.zetter.container.painting.PaintingFrameBuffer;
import com.dantaeusb.zetter.network.ClientHandler;
import com.dantaeusb.zetter.network.ServerHandler;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.network.NetworkEvent;

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
    public static CPaintingFrameBufferPacket readPacketData(PacketBuffer buf) {
        CPaintingFrameBufferPacket packet = new CPaintingFrameBufferPacket();

        long frameStartTime = buf.readLong();
        ByteBuf bufferData = buf.readBytes(buf.writerIndex() - buf.readerIndex());

        Zetter.LOG.warn(bufferData);
        packet.paintingFrameBuffer = new PaintingFrameBuffer(frameStartTime, bufferData.nioBuffer());

        return packet;
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    public void writePacketData(PacketBuffer buf) {
        buf.writeLong(this.paintingFrameBuffer.getFrameStartTime());
        buf.writeBytes(this.paintingFrameBuffer.getBufferData());
    }

    public PaintingFrameBuffer getFrameBuffer() {
        return this.paintingFrameBuffer;
    }

    public static void handle(final CPaintingFrameBufferPacket packetIn, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.setPacketHandled(true);

        final ServerPlayerEntity sendingPlayer = ctx.getSender();
        if (sendingPlayer == null) {
            Zetter.LOG.warn("EntityPlayerMP was null when CPaintingUpdatePacket was received");
        }

        ctx.enqueueWork(() -> ServerHandler.processFrameBuffer(packetIn, sendingPlayer));
    }
}