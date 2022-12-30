package me.dantaeusb.zetter.core;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.storage.CanvasDataType;
import me.dantaeusb.zetter.storage.DummyCanvasData;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.*;

import java.util.function.Supplier;

public class ZetterRegistries
{
    public static final ResourceLocation CANVAS_TYPE_REGISTRY_NAME = new ResourceLocation(Zetter.MOD_ID, "canvas_type");

    public static final DeferredRegister<CanvasDataType<?>> CANVAS_TYPE_REGISTRY_TYPE = DeferredRegister.create(CANVAS_TYPE_REGISTRY_NAME, Zetter.MOD_ID);
    public static final Supplier<IForgeRegistry<CanvasDataType<?>>> CANVAS_TYPE = CANVAS_TYPE_REGISTRY_TYPE.makeRegistry(
            () ->
            {
                RegistryBuilder<CanvasDataType<?>> builder = new RegistryBuilder<>();
                builder.disableOverrides().setDefaultKey(new ResourceLocation(Zetter.MOD_ID, DummyCanvasData.TYPE));
                return builder;
            }
    );

    public static void init(IEventBus bus) {
        CANVAS_TYPE_REGISTRY_TYPE.register(bus);
    }
}