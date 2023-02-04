package me.dantaeusb.zetter.storage;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.registries.ForgeRegistryEntry;


public class CanvasDataType<T extends AbstractCanvasData> extends ForgeRegistryEntry<CanvasDataType<?>> {
    public final CanvasDataBuilder<T> builder;

    public CanvasDataType(
            CanvasDataBuilder<T> builder
    ) {
        this.builder = builder;
    }

    public T createFresh(AbstractCanvasData.Resolution resolution, int width, int height) {
        return this.builder.createFresh(resolution, width, height);
    }

    public T createWrap(AbstractCanvasData.Resolution resolution, int width, int height, byte[] color) {
        return this.builder.createWrap(resolution, width, height, color);
    }

    public T loadFromNbt(CompoundNBT compoundTag) {
        return this.builder.load(compoundTag);
    }

    public T readPacketData(PacketBuffer byteBuf) {
        return this.builder.readPacketData(byteBuf);
    }

    public void writePacketData(T canvasData, PacketBuffer byteBuf) {
        this.builder.writePacketData(canvasData, byteBuf);
    }
}
