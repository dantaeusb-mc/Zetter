package me.dantaeusb.zetter.network.packet;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.item.PaletteItem;
import me.dantaeusb.zetter.menu.ArtistTableMenu;
import me.dantaeusb.zetter.network.ServerHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class CPaletteUseCanvasHolderPacket {
    private final int canvasHolderId;
    private final ItemStack paletteStack;

    public CPaletteUseCanvasHolderPacket(int canvasHolderId, ItemStack paletteStack) {
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
    public static CPaletteUseCanvasHolderPacket readPacketData(FriendlyByteBuf networkBuffer) {
        int canvasHolderId = networkBuffer.readInt();
        ItemStack paletteStack = networkBuffer.readItem();

        if (paletteStack.isEmpty() || !(paletteStack.getItem() instanceof PaletteItem)) {
            throw new IllegalArgumentException("Invalid palette item in packet: " + paletteStack);
        }

        return new CPaletteUseCanvasHolderPacket(canvasHolderId, paletteStack);
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    public void writePacketData(FriendlyByteBuf networkBuffer) {
        networkBuffer.writeInt(this.canvasHolderId);
        networkBuffer.writeItem(this.paletteStack);
    }

    public static void handle(final CPaletteUseCanvasHolderPacket packetIn, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        LogicalSide sideReceived = ctx.getDirection().getReceptionSide();
        ctx.setPacketHandled(true);

        if (sideReceived != LogicalSide.SERVER) {
            Zetter.LOG.warn("CPaletteUseCanvasHolderPacket received on wrong side:" + ctx.getDirection().getReceptionSide());
            return;
        }

        final ServerPlayer sendingPlayer = ctx.getSender();
        if (sendingPlayer == null) {
            Zetter.LOG.warn("EntityPlayerMP was null when CPaletteUseCanvasHolderPacket was received");
        }

        ctx.enqueueWork(() -> ServerHandler.processPaletteUseCanvasHolder(packetIn, sendingPlayer));
    }

    @Override
    public String toString()
    {
        return "CPaletteUseCanvasHolderPacket[canvasHolderId=" + this.canvasHolderId + ",paletteUuid=" + PaletteItem.getPaletteUuid(this.paletteStack) + "]";
    }
}