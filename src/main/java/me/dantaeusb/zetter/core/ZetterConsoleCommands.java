package me.dantaeusb.zetter.core;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.server.command.ZetterCommand;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Zetter.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ZetterConsoleCommands {
    @SubscribeEvent
    public static void onCommandsRegister(RegisterCommandsEvent event)
    {
        new ZetterCommand(event.getDispatcher());
    }
}