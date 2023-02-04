package me.dantaeusb.zetter.network.packet;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.network.ClientHandler;
import net.minecraft.network.PacketBuffer;
import net.minecraft.world.World;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.LogicalSidedProvider;
import net.minecraftforge.fml.network.NetworkEvent;

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
public class SEaselResetPacket {
    public final int easelEntityId;

    public SEaselResetPacket(int easelEntityId) {
        this.easelEntityId = easelEntityId;
    }

    /**
     * Reads the raw packet data from the data stream.
     */
    public static SEaselResetPacket readPacketData(PacketBuffer networkBuffer) {
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
    public void writePacketData(PacketBuffer networkBuffer) {
        networkBuffer.writeInt(this.easelEntityId);
    }

    public static void handle(final SEaselResetPacket packetIn, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        LogicalSide sideReceived = ctx.getDirection().getReceptionSide();
        ctx.setPacketHandled(true);

        Optional<World> clientWorld = LogicalSidedProvider.CLIENTWORLD.get(sideReceived);
        if (!clientWorld.isPresent()) {
            Zetter.LOG.warn("SEaselReset context could not provide a ClientWorld.");
            return;
        }

        ctx.enqueueWork(() -> ClientHandler.processEaselReset(packetIn, clientWorld.get()));
    }

    @Override
    public String toString()
    {
        return "SEaselReset[easel=" + this.easelEntityId + "]";
    }
}