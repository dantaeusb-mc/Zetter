package me.dantaeusb.zetter.server.command;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import me.dantaeusb.zetter.core.Helper;
import net.minecraft.commands.CommandRuntimeException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.io.IOException;

/**
 * Need a lot of magic here
 */
public class ExportServerCommand {
    private static final DynamicCommandExceptionType ERROR_PAINTING_NOT_FOUND = new DynamicCommandExceptionType((code) -> {
        return Component.translatable("console.zetter.error.painting_not_found", code);
    });

    private static final DynamicCommandExceptionType ERROR_CANNOT_CREATE_FILE = new DynamicCommandExceptionType((code) -> {
        return Component.translatable("console.zetter.error.file_write_error", code);
    });

    static ArgumentBuilder<CommandSourceStack, ?> register() {
        return Commands.literal("export")
            .requires(cs -> cs.hasPermission(Commands.LEVEL_GAMEMASTERS))
            .then(
                Commands.literal("server")
                    .then(
                        Commands.argument("painting", PaintingLookupArgument.painting())
                            .executes(ctx -> execute(
                                ctx.getSource(),
                                ctx.getSource().getPlayer(),
                                ctx.getSource().getLevel(),
                                PaintingLookupArgument.getPaintingInput(ctx, "painting")
                            ))
                    )
            );
    }

    private static int execute(CommandSourceStack source, Player player, Level level, PaintingInput paintingInput) throws CommandRuntimeException, CommandSyntaxException {
        if (!paintingInput.hasPaintingData(level)) {
            throw ERROR_PAINTING_NOT_FOUND.create(paintingInput.getPaintingCode());
        }

        try {
            Helper.exportPainting(level.getServer().getServerDirectory(), paintingInput.getPaintingCode(), paintingInput.getPaintingData());
        } catch (IOException e) {
            throw ERROR_CANNOT_CREATE_FILE.create(e.getMessage());
        }

        return 1;
    }
}
