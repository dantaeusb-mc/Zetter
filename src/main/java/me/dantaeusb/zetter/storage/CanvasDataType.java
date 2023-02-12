package me.dantaeusb.zetter.storage;

import net.minecraft.network.PacketBuffer;
import net.minecraftforge.registries.ForgeRegistryEntry;

public class CanvasDataType<T extends AbstractCanvasData> extends ForgeRegistryEntry<CanvasDataType<?>> {
    public final CanvasDataBuilder<T> builder;

    public CanvasDataType(
            CanvasDataBuilder<T> builder
    ) {
        this.builder = builder;
    }

    /**
     * In pre-1.18 versions, object handles loading itself
     * But it needs a supplier
     *
     * @param canvasCode
     * @return
     */
    public T supply(String canvasCode) {
        return this.builder.supply(canvasCode);
    }

    public T createFresh(String canvasCode, AbstractCanvasData.Resolution resolution, int width, int height) {
        return this.builder.createFresh(canvasCode, resolution, width, height);
    }

    public T createWrap(String canvasCode, AbstractCanvasData.Resolution resolution, int width, int height, byte[] color) {
        return this.builder.createWrap(canvasCode, resolution, width, height, color);
    }

    public T readPacketData(PacketBuffer byteBuf) {
        return this.builder.readPacketData(byteBuf);
    }

    public void writePacketData(String canvasCode, T canvasData, PacketBuffer byteBuf) {
        this.builder.writePacketData(canvasCode, canvasData, byteBuf);
    }
}
