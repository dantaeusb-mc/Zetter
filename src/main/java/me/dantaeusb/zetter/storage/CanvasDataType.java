package me.dantaeusb.zetter.storage;

import me.dantaeusb.zetter.core.ZetterRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistryEntry;
import net.minecraftforge.registries.IForgeRegistryEntry;
import org.jetbrains.annotations.Nullable;

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
