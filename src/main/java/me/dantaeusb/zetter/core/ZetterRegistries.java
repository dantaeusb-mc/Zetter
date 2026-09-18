package me.dantaeusb.zetter.core;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.storage.CanvasDataType;
import me.dantaeusb.zetter.storage.DummyCanvasData;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ZetterRegistries
{
    public static final ResourceKey<Registry<CanvasDataType<?>>> CANVAS_TYPE_REGISTRY_KEY = ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(Zetter.MOD_ID, "canvas_type"));

    public static final DeferredRegister<CanvasDataType<?>> CANVAS_TYPE_REGISTRY_TYPE = DeferredRegister.create(CANVAS_TYPE_REGISTRY_KEY, Zetter.MOD_ID);

    public static final Registry<CanvasDataType<?>> CANVAS_TYPE_REGISTRY = CANVAS_TYPE_REGISTRY_TYPE.makeRegistry(
            builder -> builder.defaultKey(ResourceLocation.fromNamespaceAndPath(Zetter.MOD_ID, DummyCanvasData.TYPE))
    );

    public static final Supplier<Registry<CanvasDataType<?>>> CANVAS_TYPE = () -> CANVAS_TYPE_REGISTRY;

    public static void init(IEventBus bus) {
        CANVAS_TYPE_REGISTRY_TYPE.register(bus);
    }
}