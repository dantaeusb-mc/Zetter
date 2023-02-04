package me.dantaeusb.zetter.storage;


import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;

public interface CanvasDataBuilder<T extends AbstractCanvasData> {
    T createFresh(AbstractCanvasData.Resolution resolution, int width, int height);
    T createWrap(AbstractCanvasData.Resolution resolution, int width, int height, byte[] color);
    T load(CompoundNBT compoundTag);
    T readPacketData(PacketBuffer byteBuf);
    void writePacketData(T canvasData, PacketBuffer byteBuf);
}
