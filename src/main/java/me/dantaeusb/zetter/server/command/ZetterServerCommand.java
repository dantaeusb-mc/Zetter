package me.dantaeusb.zetter.server.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.dantaeusb.zetter.Zetter;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;

public class ZetterServerCommand {
    public ZetterServerCommand(CommandDispatcher<CommandSource> dispatcher)
    {
        dispatcher.register(
            Commands.literal(Zetter.MOD_ID)
                .then(RestoreCommand.register())
                .then(ExportServerCommand.register())
                .then(ExportClientCommand.register())
        );
    }
}
