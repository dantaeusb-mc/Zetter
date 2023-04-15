package me.dantaeusb.zetter.core;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.storage.CanvasData;
import me.dantaeusb.zetter.storage.CanvasDataType;
import me.dantaeusb.zetter.storage.DummyCanvasData;
import me.dantaeusb.zetter.storage.PaintingData;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ZetterCanvasTypes
{
    public static final DeferredRegister<CanvasDataType<?>> CANVAS_TYPES = DeferredRegister.create(ZetterRegistries.CANVAS_TYPE_REGISTRY_NAME, Zetter.MOD_ID);

    public static final RegistryObject<CanvasDataType<DummyCanvasData>> DUMMY = CANVAS_TYPES.register(DummyCanvasData.TYPE, () -> new CanvasDataType<>(
        new ResourceLocation(Zetter.MOD_ID, DummyCanvasData.TYPE),
        DummyCanvasData.BUILDER,
        DummyCanvasData.class));
    public static final RegistryObject<CanvasDataType<CanvasData>> CANVAS = CANVAS_TYPES.register(CanvasData.TYPE, () -> new CanvasDataType<>(
        new ResourceLocation(Zetter.MOD_ID, CanvasData.TYPE),
        CanvasData.BUILDER,
        CanvasData.class));
    public static final RegistryObject<CanvasDataType<PaintingData>> PAINTING = CANVAS_TYPES.register(PaintingData.TYPE, () -> new CanvasDataType<>(
        new ResourceLocation(Zetter.MOD_ID, PaintingData.TYPE),
        PaintingData.BUILDER,
        PaintingData.class));

    public static void init(IEventBus bus) {
        CANVAS_TYPES.register(bus);
    }
}