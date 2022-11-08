package me.dantaeusb.zetter.core;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.storage.*;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ZetterCanvasTypes
{
    private static final DeferredRegister<CanvasDataType<?>> CANVAS_TYPES = DeferredRegister.create(ZetterRegistries.CANVAS_TYPE.get(), Zetter.MOD_ID);

    public static final RegistryObject<CanvasDataType<DummyCanvasData>> DUMMY = CANVAS_TYPES.register(DummyCanvasData.TYPE, () -> new CanvasDataType<>(
            DummyCanvasData::createDummy,
            DummyCanvasData::createWrap,
            DummyCanvasData::load,
            DummyCanvasData::readPacketData,
            DummyCanvasData::writePacketData
    ));
    public static final RegistryObject<CanvasDataType<CanvasData>> CANVAS = CANVAS_TYPES.register(CanvasData.TYPE, () -> new CanvasDataType<>(
            CanvasData::createFresh,
            CanvasData::createWrap,
            CanvasData::load,
            CanvasData::readPacketData,
            CanvasData::writePacketData
    ));
    public static final RegistryObject<CanvasDataType<PaintingData>> PAINTING = CANVAS_TYPES.register(PaintingData.TYPE, () -> new CanvasDataType<>(
            PaintingData::createFresh,
            PaintingData::createWrap,
            PaintingData::load,
            PaintingData::readPacketData,
            PaintingData::writePacketData
    ));

    public static void init(IEventBus bus) {
        CANVAS_TYPES.register(bus);
    }
}