package me.dantaeusb.zetter.network.packet;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.core.Helper;
import me.dantaeusb.zetter.network.ClientHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.LogicalSidedProvider;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Send snapshot of a canvas, used only for easel when drawing, a bit more specific
 * object that is sent more frequently than default canvases when
 * multiple players are drawing
 *
 * @todo: [MED] Do we need that since we can track on client
 * if canvas item was changed
 */
public class SEaselCanvasInitializationPacket {
    public final int easelEntityId;
    public final String canvasCode;

    public SEaselCanvasInitializationPacket(int easelEntityId, String canvasCode) {
        this.easelEntityId = easelEntityId;
        this.canvasCode = canvasCode;
    }

    /**
     * Reads the raw packet data from the data stream.
     */
    public static SEaselCanvasInitializationPacket readPacketData(FriendlyByteBuf networkBuffer) {
        try {
            final int easelEntityId = networkBuffer.readInt();
            final String canvasCode = networkBuffer.readUtf(Helper.CANVAS_CODE_MAX_LENGTH);

            return new SEaselCanvasInitializationPacket(easelEntityId, canvasCode);
        } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
            Zetter.LOG.warn("Exception while reading SEaselCanvasInitializationPacket: " + e);
            return null;
        }
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    public void writePacketData(FriendlyByteBuf networkBuffer) {
        networkBuffer.writeInt(this.easelEntityId);
        networkBuffer.writeUtf(this.canvasCode, Helper.CANVAS_CODE_MAX_LENGTH);
    }

    public static void handle(final SEaselCanvasInitializationPacket packetIn, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        LogicalSide sideReceived = ctx.getDirection().getReceptionSide();
        ctx.setPacketHandled(true);

        Optional<Level> clientWorld = LogicalSidedProvider.CLIENTWORLD.get(sideReceived);
        if (!clientWorld.isPresent()) {
            Zetter.LOG.warn("SEaselReset context could not provide a ClientWorld.");
            return;
        }

        ctx.enqueueWork(() -> ClientHandler.processEaselCanvasInitialization(packetIn, clientWorld.get()));
    }

    @Override
    public String toString()
    {
        return "SEaselCanvasInitializationPacket[easel=" + this.easelEntityId + ",canvasCode=" + this.canvasCode + "]";
    }
}