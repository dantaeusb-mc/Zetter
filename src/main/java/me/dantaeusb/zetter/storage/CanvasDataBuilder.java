package me.dantaeusb.zetter.storage;


import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

import me.dantaeusb.zetter.core.Helper;

public interface CanvasDataBuilder<T extends AbstractCanvasData> {
    default T createFresh(AbstractCanvasData.Resolution resolution, int width, int height) {
        return this.createFresh(resolution, width, height, Helper.CANVAS_COLOR);
    }
    T createFresh(AbstractCanvasData.Resolution resolution, int width, int height, int groundColor);
    T createWrap(AbstractCanvasData.Resolution resolution, int width, int height, byte[] color);
    T load(CompoundTag compoundTag);
    T readPacketData(FriendlyByteBuf byteBuf);
    void writePacketData(T canvasData, FriendlyByteBuf byteBuf);
}
