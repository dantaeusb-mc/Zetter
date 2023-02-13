package me.dantaeusb.zetter.core;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.server.command.PaintingLookupArgument;
import me.dantaeusb.zetter.server.command.ZetterServerCommand;
import net.minecraft.command.arguments.ArgumentSerializer;
import net.minecraft.command.arguments.ArgumentTypes;
import net.minecraft.command.arguments.GameProfileArgument;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Zetter.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ZetterConsoleCommands {
    @SubscribeEvent
    public static void onCommandsRegister(RegisterCommandsEvent event)
    {
        new ZetterServerCommand(event.getDispatcher());
    }

    public static void init(IEventBus bus) {
        ArgumentTypes.register("zetter:painting_lookup", PaintingLookupArgument.class, new ArgumentSerializer<>(PaintingLookupArgument::new));
    }
}