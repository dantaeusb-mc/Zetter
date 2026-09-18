package me.dantaeusb.zetter.network.packet;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.network.handling.IPayloadContext;


import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.network.ServerHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import javax.annotation.Nullable;
import java.security.InvalidParameterException;
import java.util.function.Supplier;

public class CCanvasRequestExportPacket implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {
    public static final Type<CCanvasRequestExportPacket> TYPE = new Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(me.dantaeusb.zetter.Zetter.MOD_ID, "canvas_request_export"));

    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, CCanvasRequestExportPacket> STREAM_CODEC = StreamCodec.of(
        (buf, packet) -> packet.writePacketData(buf),
        CCanvasRequestExportPacket::readPacketData
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public final @Nullable String requestCode;
    public final @Nullable String requestTitle;

    public CCanvasRequestExportPacket(@Nullable String requestCode, @Nullable String requestTitle) {
        if (requestCode == null && requestTitle == null) {
            throw new InvalidParameterException("One of the parameters should be non-null: code or title");
        }

        this.requestCode = requestCode;
        this.requestTitle = requestTitle;
    }

    /**
     * Reads the raw packet data from the data stream.
     * Seems like buf is always at least 256 bytes, so we have to process written buffer size
     */
    public static CCanvasRequestExportPacket readPacketData(FriendlyByteBuf buf) {
        try {
            String requestCode = null;

            if (buf.readBoolean()) {
                requestCode = buf.readUtf(32767);
            }

            String requestTitle = null;

            if (buf.readBoolean()) {
                requestTitle = buf.readUtf(32767);
            }

            return new CCanvasRequestExportPacket(requestCode, requestTitle);
        } catch (Exception e) {
            Zetter.LOG.warn("Exception while reading CCanvasRequestExportPacket: " + e);
            return null;
        }
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    public void writePacketData(FriendlyByteBuf buf) {
        if (this.requestCode != null) {
            buf.writeBoolean(true);
            buf.writeUtf(this.requestCode, 32767);
        } else {
            buf.writeBoolean(false);
        }

        if (this.requestTitle != null) {
            buf.writeBoolean(true);
            buf.writeUtf(this.requestTitle, 32767);
        } else {
            buf.writeBoolean(false);
        }
    }

    public static void handle(final CCanvasRequestExportPacket packetIn, IPayloadContext context) {
        final ServerPlayer sendingPlayer = (net.minecraft.server.level.ServerPlayer) context.player();
        if (sendingPlayer == null) {
            Zetter.LOG.warn("EntityPlayerMP was null when CCanvasRequestExportPacket was received");
        }

        context.enqueueWork(() -> ServerHandler.processCanvasExportRequest(packetIn, sendingPlayer));
    }

    @Override
    public String toString()
    {
        return "CCanvasRequestExportPacket[requestCode=" + this.requestCode + ",requestTitle=" + this.requestTitle + "]";
    }
}