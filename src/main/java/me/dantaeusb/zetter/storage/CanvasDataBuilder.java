package me.dantaeusb.zetter.storage;


import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;

public interface CanvasDataBuilder<T extends AbstractCanvasData> {
    T supply(String canvasCode);
    T createFresh(String canvasCode, AbstractCanvasData.Resolution resolution, int width, int height);
    T createWrap(String canvasCode, AbstractCanvasData.Resolution resolution, int width, int height, byte[] color);
    T readPacketData(PacketBuffer byteBuf);
    void writePacketData(String canvasCode, T canvasData, PacketBuffer byteBuf);
}
