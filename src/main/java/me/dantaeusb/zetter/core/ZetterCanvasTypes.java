package me.dantaeusb.zetter.core;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.storage.CanvasData;
import me.dantaeusb.zetter.storage.CanvasDataType;
import me.dantaeusb.zetter.storage.DummyCanvasData;
import me.dantaeusb.zetter.storage.PaintingData;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ZetterCanvasTypes
{
    public static final DeferredRegister<CanvasDataType<?>> CANVAS_TYPES = DeferredRegister.create(ZetterRegistries.CANVAS_TYPE_REGISTRY_KEY, Zetter.MOD_ID);

    public static final DeferredHolder<CanvasDataType<?>, CanvasDataType<DummyCanvasData>> DUMMY = CANVAS_TYPES.register(DummyCanvasData.TYPE, () -> new CanvasDataType<>(
        ResourceLocation.fromNamespaceAndPath(Zetter.MOD_ID, DummyCanvasData.TYPE),
        DummyCanvasData.BUILDER,
        DummyCanvasData.class));
    public static final DeferredHolder<CanvasDataType<?>, CanvasDataType<CanvasData>> CANVAS = CANVAS_TYPES.register(CanvasData.TYPE, () -> new CanvasDataType<>(
        ResourceLocation.fromNamespaceAndPath(Zetter.MOD_ID, CanvasData.TYPE),
        CanvasData.BUILDER,
        CanvasData.class));
    public static final DeferredHolder<CanvasDataType<?>, CanvasDataType<PaintingData>> PAINTING = CANVAS_TYPES.register(PaintingData.TYPE, () -> new CanvasDataType<>(
        ResourceLocation.fromNamespaceAndPath(Zetter.MOD_ID, PaintingData.TYPE),
        PaintingData.BUILDER,
        PaintingData.class));

    public static void init(IEventBus bus) {
        CANVAS_TYPES.register(bus);
    }
}