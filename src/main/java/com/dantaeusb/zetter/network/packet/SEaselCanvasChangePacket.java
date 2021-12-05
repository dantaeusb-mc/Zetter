package com.dantaeusb.zetter.network.packet;

import com.dantaeusb.zetter.Zetter;
import com.dantaeusb.zetter.network.ClientHandler;
import com.dantaeusb.zetter.storage.AbstractCanvasData;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.LogicalSidedProvider;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * @link {SSetSlotPacket}
 */
public class SEaselCanvasChangePacket {
    private int windowId;
    private ItemStack item = ItemStack.EMPTY;

    public SEaselCanvasChangePacket(int windowId, ItemStack item) {
        this.windowId = windowId;
        this.item = item.copy();
    }

    /**
     * Reads the raw packet data from the data stream.
     */
    public static SEaselCanvasChangePacket readPacketData(PacketBuffer networkBuffer) {
        try {
            final int windowId = networkBuffer.readByte();
            final ItemStack item = networkBuffer.readItem();

            return new SEaselCanvasChangePacket(windowId, item);
        } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
            Zetter.LOG.warn("Exception while reading SEaselCanvasChangePacket: " + e);
            return null;
        }
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    public void writePacketData(PacketBuffer networkBuffer) {
        networkBuffer.writeByte(this.windowId);
        networkBuffer.writeItem(this.item);
    }

    public int getWindowId() {
        return this.windowId;
    }

    public ItemStack getItem() {
        return this.item;
    }

    public static void handle(final SEaselCanvasChangePacket packetIn, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        LogicalSide sideReceived = ctx.getDirection().getReceptionSide();
        ctx.setPacketHandled(true);

        Optional<ClientWorld> clientWorld = LogicalSidedProvider.CLIENTWORLD.get(sideReceived);
        if (!clientWorld.isPresent()) {
            Zetter.LOG.warn("SEaselCanvasChangePacket context could not provide a ClientWorld.");
            return;
        }

        ctx.enqueueWork(() -> ClientHandler.processEaselCanvasUpdate(packetIn, clientWorld.get()));
    }

    @Override
    public String toString()
    {
        return "SEaselCanvasChangePacket[windowId=" + this.windowId + ",stack=" + this.item + "]";
    }
}