package me.dantaeusb.zetter.network.packet;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.core.Helper;
import me.dantaeusb.zetter.network.ServerHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.network.NetworkEvent;

import javax.annotation.Nullable;
import java.security.InvalidParameterException;
import java.util.function.Supplier;

public class CCanvasRequestExportPacket {
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

    public static void handle(final CCanvasRequestExportPacket packetIn, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.setPacketHandled(true);

        final ServerPlayer sendingPlayer = ctx.getSender();
        if (sendingPlayer == null) {
            Zetter.LOG.warn("EntityPlayerMP was null when CCanvasRequestExportPacket was received");
        }

        ctx.enqueueWork(() -> ServerHandler.processCanvasExportRequest(packetIn, sendingPlayer));
    }

    @Override
    public String toString()
    {
        return "CCanvasRequestExportPacket[requestCode=" + this.requestCode + ",requestTitle=" + this.requestTitle + "]";
    }
}