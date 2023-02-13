package me.dantaeusb.zetter.core;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.storage.CanvasData;
import me.dantaeusb.zetter.storage.CanvasDataType;
import me.dantaeusb.zetter.storage.DummyCanvasData;
import me.dantaeusb.zetter.storage.PaintingData;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;

import static me.dantaeusb.zetter.core.ZetterRegistries.CANVAS_TYPE_REGISTRY;
import static me.dantaeusb.zetter.core.ZetterRegistries.HACKY_TYPE;

public class ZetterCanvasTypes
{
    public static final RegistryObject<CanvasDataType<DummyCanvasData>> DUMMY = CANVAS_TYPE_REGISTRY.register(DummyCanvasData.TYPE, () -> new CanvasDataType<>(
        DummyCanvasData.BUILDER
    ));
    public static final RegistryObject<CanvasDataType<CanvasData>> CANVAS = CANVAS_TYPE_REGISTRY.register(CanvasData.TYPE, () -> new CanvasDataType<>(
        CanvasData.BUILDER
    ));
    public static final RegistryObject<CanvasDataType<PaintingData>> PAINTING = CANVAS_TYPE_REGISTRY.register(PaintingData.TYPE, () -> new CanvasDataType<>(
        PaintingData.BUILDER
    ));

    public static void init(IEventBus bus) {
        //
    }
}