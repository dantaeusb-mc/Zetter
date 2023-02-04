package me.dantaeusb.zetter.server.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.dantaeusb.zetter.Zetter;
import net.minecraft.commands.CommandSourceStack;

public class ZetterServerCommand {
    public ZetterServerCommand(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        dispatcher.register(
            LiteralArgumentBuilder.<CommandSourceStack>literal(Zetter.MOD_ID)
                .then(RestoreCommand.register())
                .then(ExportServerCommand.register())
        );
    }
}
