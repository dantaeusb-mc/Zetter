package me.dantaeusb.zetter.network.packet;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.network.ServerHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class CPaletteUpdatePacket {
  private UUID paletteUuid;
  private int slotIndex;
  private int color;

  public CPaletteUpdatePacket() {
  }

  public CPaletteUpdatePacket(UUID paletteUuid, int slotIndex, int color) {
    this.paletteUuid = paletteUuid;
    this.slotIndex = slotIndex;
    this.color = color;
  }

  /**
   * Reads the raw packet data from the data stream.
   */
  public static CPaletteUpdatePacket readPacketData(FriendlyByteBuf buf) {
    CPaletteUpdatePacket packet = new CPaletteUpdatePacket();

    packet.paletteUuid = buf.readUUID();
    packet.slotIndex = buf.readInt();
    packet.color = buf.readInt();

    return packet;
  }

  /**
   * Writes the raw packet data to the data stream.
   */
  public void writePacketData(FriendlyByteBuf buf) {
    buf.writeUUID(this.paletteUuid);
    buf.writeInt(this.slotIndex);
    buf.writeInt(this.color);
  }

  public UUID getPaletteUuid() {
    return this.paletteUuid;
  }

  public int getSlotIndex() {
    return this.slotIndex;
  }

  public int getColor() {
    return this.color;
  }

  public static void handle(final CPaletteUpdatePacket packetIn, Supplier<NetworkEvent.Context> ctxSupplier) {
    NetworkEvent.Context ctx = ctxSupplier.get();
    LogicalSide sideReceived = ctx.getDirection().getReceptionSide();
    ctx.setPacketHandled(true);

    if (sideReceived != LogicalSide.SERVER) {
      Zetter.LOG.warn("PaletteUpdatePacket received on wrong side:" + ctx.getDirection().getReceptionSide());
      return;
    }

    final ServerPlayer sendingPlayer = ctx.getSender();
    if (sendingPlayer == null) {
      Zetter.LOG.warn("EntityPlayerMP was null when PaletteUpdatePacket was received");
    }

    ctx.enqueueWork(() -> ServerHandler.processPaletteUpdate(packetIn, sendingPlayer));
  }
}