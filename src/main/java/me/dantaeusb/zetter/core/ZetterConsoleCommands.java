package me.dantaeusb.zetter.core;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.server.command.PaintingLookupArgument;
import me.dantaeusb.zetter.server.command.ZetterClientCommand;
import me.dantaeusb.zetter.server.command.ZetterServerCommand;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@EventBusSubscriber(modid = Zetter.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class ZetterConsoleCommands {
    private static final DeferredRegister<ArgumentTypeInfo<?, ?>> ARGUMENT_TYPES_INFO = DeferredRegister.create(Registries.COMMAND_ARGUMENT_TYPE, Zetter.MOD_ID);

    public static final DeferredHolder<ArgumentTypeInfo<?, ?>, SingletonArgumentInfo<PaintingLookupArgument>> ARGUMENT_TYPE_PAINTING_LOOKUP = ARGUMENT_TYPES_INFO.register(
        "painting_lookup",
        () -> ArgumentTypeInfos.registerByClass(PaintingLookupArgument.class, SingletonArgumentInfo.contextFree(PaintingLookupArgument::painting))
    );

    @SubscribeEvent
    public static void onCommandsRegister(RegisterCommandsEvent event)
    {
        new ZetterServerCommand(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onClientCommandsRegister(RegisterClientCommandsEvent event) {
        new ZetterClientCommand(event.getDispatcher());
    }

    public static void init(IEventBus bus) {
        ARGUMENT_TYPES_INFO.register(bus);
    }
}