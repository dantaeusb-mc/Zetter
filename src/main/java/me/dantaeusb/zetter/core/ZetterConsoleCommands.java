package me.dantaeusb.zetter.core;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.server.command.PaintingLookupArgument;
import me.dantaeusb.zetter.server.command.ZetterCommand;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod.EventBusSubscriber(modid = Zetter.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ZetterConsoleCommands {
    private static final DeferredRegister<ArgumentTypeInfo<?, ?>> ARGUMENT_TYPES_INFO = DeferredRegister.create(ForgeRegistries.COMMAND_ARGUMENT_TYPES, Zetter.MOD_ID);

    public static final RegistryObject<SingletonArgumentInfo<PaintingLookupArgument>> ARGUMENT_TYPE_PAINTING_LOOKUP = ARGUMENT_TYPES_INFO.register(
        "painting_lookup",
        () -> ArgumentTypeInfos.registerByClass(PaintingLookupArgument.class, SingletonArgumentInfo.contextFree(PaintingLookupArgument::painting))
    );

    @SubscribeEvent
    public static void onCommandsRegister(RegisterCommandsEvent event)
    {
        new ZetterCommand(event.getDispatcher());
    }

    public static void init(IEventBus bus) {
        ARGUMENT_TYPES_INFO.register(bus);
    }
}