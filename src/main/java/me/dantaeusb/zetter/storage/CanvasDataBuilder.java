package me.dantaeusb.zetter.storage;


import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

public interface CanvasDataBuilder<T extends AbstractCanvasData> {
    T createFresh(AbstractCanvasData.Resolution resolution, int width, int height);
    T createWrap(AbstractCanvasData.Resolution resolution, int width, int height, byte[] color);
    T load(CompoundTag compoundTag);
    T readPacketData(FriendlyByteBuf byteBuf);
    void writePacketData(T canvasData, FriendlyByteBuf byteBuf);
}
