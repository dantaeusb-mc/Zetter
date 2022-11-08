package me.dantaeusb.zetter.storage;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

public class CanvasDataType<T extends AbstractCanvasData> {
    private final CanvasDataSupplier<T> supplier;
    private final CanvasDataWrapper<T> wrapper;
    private final CanvasDataNbtLoader<T> nbtLoader;
    private final CanvasDataNetworkHandler<T> networkHandler;
    private final CanvasDataNetworkWriter<T> networkWriter;

    public CanvasDataType(
            CanvasDataSupplier<T> supplier, CanvasDataWrapper<T> wrapper, CanvasDataNbtLoader<T> nbtLoader,
            CanvasDataNetworkHandler<T> networkHandler, CanvasDataNetworkWriter<T> networkWriter
    ) {
        this.supplier = supplier;
        this.wrapper = wrapper;
        this.nbtLoader = nbtLoader;
        this.networkHandler = networkHandler;
        this.networkWriter = networkWriter;
    }

    public T createFresh(AbstractCanvasData.Resolution resolution, int width, int height) {
        return this.supplier.createFresh(resolution, width, height);
    }

    public T createWrap(AbstractCanvasData.Resolution resolution, int width, int height, byte[] color) {
        return this.wrapper.createWrap(resolution, width, height, color);
    }

    public T loadFromNbt(CompoundTag compoundTag) {
        return this.nbtLoader.load(compoundTag);
    }

    public T readPacketData(FriendlyByteBuf byteBuf) {
        return this.networkHandler.readPacketData(byteBuf);
    }

    public void writePacketData(T canvasData, FriendlyByteBuf byteBuf) {
        this.networkWriter.writePacketData(canvasData, byteBuf);
    }

    @FunctionalInterface
    public interface CanvasDataSupplier<T extends AbstractCanvasData> {
        T createFresh(AbstractCanvasData.Resolution resolution, int width, int height);
    }

    @FunctionalInterface
    public interface CanvasDataWrapper<T extends AbstractCanvasData> {
        T createWrap(AbstractCanvasData.Resolution resolution, int width, int height, byte[] color);
    }

    @FunctionalInterface
    public interface CanvasDataNbtLoader<T extends AbstractCanvasData> {
        T load(CompoundTag compoundTag);
    }

    @FunctionalInterface
    public interface CanvasDataNetworkHandler<T extends AbstractCanvasData> {
        T readPacketData(FriendlyByteBuf byteBuf);
    }

    @FunctionalInterface
    public interface CanvasDataNetworkWriter<T extends AbstractCanvasData> {
        void writePacketData(T canvasData, FriendlyByteBuf byteBuf);
    }
}
