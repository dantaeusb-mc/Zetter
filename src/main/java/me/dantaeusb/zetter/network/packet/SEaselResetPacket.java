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
 * Send snapshot of a canvas, used only for easel when drawing, a bit more specific
 * object that is sent more frequently than default canvases when
 * multiple players are drawing
 *
 * @todo: [MED] Do we need that since we can track on client
 * if canvas item was changed
 */
public class SEaselResetPacket implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {
    public static final Type<SEaselResetPacket> TYPE = new Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(me.dantaeusb.zetter.Zetter.MOD_ID, "easel_reset"));

    public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, SEaselResetPacket> STREAM_CODEC = StreamCodec.of(
        (buf, packet) -> packet.writePacketData(buf),
        SEaselResetPacket::readPacketData
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public final int easelEntityId;

    public SEaselResetPacket(int easelEntityId) {
        this.easelEntityId = easelEntityId;
    }

    /**
     * Reads the raw packet data from the data stream.
     */
    public static SEaselResetPacket readPacketData(FriendlyByteBuf networkBuffer) {
        try {
            final int easelEntityId = networkBuffer.readInt();

            return new SEaselResetPacket(easelEntityId);
        } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
            Zetter.LOG.warn("Exception while reading SEaselStateSync: " + e);
            return null;
        }
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    public void writePacketData(FriendlyByteBuf networkBuffer) {
        networkBuffer.writeInt(this.easelEntityId);
    }

    public static void handle(final SEaselResetPacket packetIn, IPayloadContext context) {
        Optional<Level> clientWorld = Optional.of(context.player().level());
        if (!clientWorld.isPresent()) {
            Zetter.LOG.warn("SEaselReset context could not provide a ClientWorld.");
            return;
        }

        context.enqueueWork(() -> ClientHandler.processEaselReset(packetIn, clientWorld.get()));
    }

    @Override
    public String toString()
    {
        return "SEaselReset[easel=" + this.easelEntityId + "]";
    }
}