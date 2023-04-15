package me.dantaeusb.zetter.storage;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public class CanvasDataType<T extends AbstractCanvasData> {
    public final ResourceLocation resourceLocation;
    public final CanvasDataBuilder<T> builder;
    public final Class<T> clazz;

    public CanvasDataType(
            ResourceLocation resourceLocation,
            CanvasDataBuilder<T> builder,
            Class<T> clazz) {
        this.resourceLocation = resourceLocation;
        this.builder = builder;
        this.clazz = clazz;
    }

    public T createFresh(AbstractCanvasData.Resolution resolution, int width, int height) {
        return this.builder.createFresh(resolution, width, height);
    }

    public T createWrap(AbstractCanvasData.Resolution resolution, int width, int height, byte[] color) {
        return this.builder.createWrap(resolution, width, height, color);
    }

    public T loadFromNbt(CompoundTag compoundTag) {
        return this.builder.load(compoundTag);
    }

    public T readPacketData(FriendlyByteBuf byteBuf) {
        return this.builder.readPacketData(byteBuf);
    }

    public void writePacketData(T canvasData, FriendlyByteBuf byteBuf) {
        this.builder.writePacketData(canvasData, byteBuf);
    }
}
