package me.dantaeusb.zetter.network.packet;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.item.PaletteItem;
import me.dantaeusb.zetter.network.ClientHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.LogicalSidedProvider;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;

import java.util.Optional;
import java.util.function.Supplier;

public class SCanvasHolderAcceptPacket {
    private final int canvasHolderId;
    private final ItemStack paletteStack;

    public SCanvasHolderAcceptPacket(int canvasHolderId, ItemStack paletteStack) {
        this.canvasHolderId = canvasHolderId;
        this.paletteStack = paletteStack;
    }

    public int getCanvasHolderId() {
        return this.canvasHolderId;
    }

    public ItemStack getPaletteStack() {
        return this.paletteStack;
    }

    /**
     * Reads the raw packet data from the data stream.
     */
    public static SCanvasHolderAcceptPacket readPacketData(FriendlyByteBuf networkBuffer) {
        int canvasHolderId = networkBuffer.readInt();
        ItemStack paletteStack = networkBuffer.readItem();

        if (paletteStack.isEmpty() || !(paletteStack.getItem() instanceof PaletteItem)) {
            throw new IllegalArgumentException("Invalid palette item in packet: " + paletteStack);
        }

        return new SCanvasHolderAcceptPacket(canvasHolderId, paletteStack);
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    public void writePacketData(FriendlyByteBuf networkBuffer) {
        networkBuffer.writeInt(this.canvasHolderId);
        networkBuffer.writeItem(this.paletteStack);
    }

    public static void handle(final SCanvasHolderAcceptPacket packetIn, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        LogicalSide sideReceived = ctx.getDirection().getReceptionSide();
        ctx.setPacketHandled(true);

        Optional<Level> clientWorld = LogicalSidedProvider.CLIENTWORLD.get(sideReceived);
        if (!clientWorld.isPresent()) {
            Zetter.LOG.warn("SEaselReset context could not provide a ClientWorld.");
            return;
        }

        ctx.enqueueWork(() -> ClientHandler.processCanvasHolderAcceptPacket(packetIn, clientWorld.get()));
    }

    @Override
    public String toString() {
        return "SCanvasHolderAcceptPacket[canvasHolderId=" + this.canvasHolderId + ",paletteUuid=" + PaletteItem.getPaletteUuid(this.paletteStack) + "]";
    }
}