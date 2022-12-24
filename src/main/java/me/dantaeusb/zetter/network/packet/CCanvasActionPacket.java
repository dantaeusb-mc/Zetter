package me.dantaeusb.zetter.network.packet;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.entity.item.state.representation.CanvasAction;
import me.dantaeusb.zetter.network.ServerHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.function.Supplier;

/**
 * Painting update - get frame buffer from client when they're making changes
 */
public class CCanvasActionPacket {
    public final int easelEntityId;
    public final Queue<CanvasAction> paintingActions;

    public CCanvasActionPacket(int entityId) {
        this.easelEntityId = entityId;
        this.paintingActions = new ArrayDeque<>();
    }

    public CCanvasActionPacket(int entityId, Queue<CanvasAction> paintingActionBuffers) {
        this.easelEntityId = entityId;
        this.paintingActions = paintingActionBuffers;
    }

    /**
     * Reads the raw packet data from the data stream.
     * Seems like buffer is always at least 256 bytes, so we have to process written buffer size
     */
    public static CCanvasActionPacket readPacketData(FriendlyByteBuf networkBuffer) {
        int entityId = networkBuffer.readInt();
        int actionBuffersCount = networkBuffer.readInt();

        CCanvasActionPacket packet = new CCanvasActionPacket(entityId);

        for (int i = 0; i < actionBuffersCount; i++) {
            @Nullable CanvasAction action = CanvasAction.readPacketData(networkBuffer);

            if (action != null) {
                packet.paintingActions.add(action);
            } else {
                // @todo: [MED] Figure out why this happens. Guaranteed to happen after debugger pause!
                // But also happens randomly when drawing a lot
                Zetter.LOG.error("Cannot retrieve actions from buffer");
            }
        }

        return packet;
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    public void writePacketData(FriendlyByteBuf networkBuffer) {
        networkBuffer.writeInt(this.easelEntityId);
        networkBuffer.writeInt(this.paintingActions.size());

        for (CanvasAction actionBuffer : this.paintingActions) {
            CanvasAction.writePacketData(actionBuffer, networkBuffer);
        }
    }

    public static void handle(final CCanvasActionPacket packetIn, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.setPacketHandled(true);

        final ServerPlayer sendingPlayer = ctx.getSender();
        if (sendingPlayer == null) {
            Zetter.LOG.warn("EntityPlayerMP was null when CPaintingUpdatePacket was received");
        }

        ctx.enqueueWork(() -> ServerHandler.processAction(packetIn, sendingPlayer));
    }
}