package me.dantaeusb.zetter.network.packet;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.network.handling.IPayloadContext;


import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.network.ClientHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;


import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Needed when several players are editing, to notify other players
 * that one of them canceled an action
 */
public class SCanvasHistoryActionPacket implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {
    public static final Type<SCanvasHistoryActionPacket> TYPE = new Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(me.dantaeusb.zetter.Zetter.MOD_ID, "s_canvas_history_action"));

    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, SCanvasHistoryActionPacket> STREAM_CODEC = StreamCodec.of(
        (buf, packet) -> packet.writePacketData(buf),
        SCanvasHistoryActionPacket::readPacketData
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public final int easelEntityId;
    public final int actionId;
    public final boolean canceled;

    public SCanvasHistoryActionPacket(int easelEntityId, int actionId, boolean canceled) {
        this.easelEntityId = easelEntityId;
        this.actionId = actionId;
        this.canceled = canceled;
    }

    /**
     * Reads the raw packet data from the data stream.
     * Seems like buffer is always at least 256 bytes, so we have to process written buffer size
     */
    public static SCanvasHistoryActionPacket readPacketData(FriendlyByteBuf buffer) {
        final int easelEntityId = buffer.readInt();
        final int actionId = buffer.readInt();
        final boolean canceled = buffer.readBoolean();

        return new SCanvasHistoryActionPacket(easelEntityId, actionId, canceled);
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    public void writePacketData(FriendlyByteBuf buffer) {
        buffer.writeInt(this.easelEntityId);
        buffer.writeInt(this.actionId);
        buffer.writeBoolean(this.canceled);
    }

    public static void handle(final SCanvasHistoryActionPacket packetIn, IPayloadContext context) {
        Optional<Level> clientWorld = Optional.of(context.player().level());
        if (!clientWorld.isPresent()) {
            Zetter.LOG.warn("SCanvasHistoryActionPacket context could not provide a ClientWorld.");
            return;
        }

        context.enqueueWork(() -> ClientHandler.processCanvasHistory(packetIn, clientWorld.get()));
    }
}