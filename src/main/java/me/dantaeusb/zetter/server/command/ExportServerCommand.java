package me.dantaeusb.zetter.server.command;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import me.dantaeusb.zetter.core.Helper;
import net.minecraft.command.CommandException;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;

import java.io.IOException;

/**
 * Need a lot of magic here
 */
public class ExportServerCommand {
    private static final DynamicCommandExceptionType ERROR_PAINTING_NOT_FOUND = new DynamicCommandExceptionType((code) -> {
        return new TranslationTextComponent("console.zetter.error.painting_not_found", code);
    });

    private static final DynamicCommandExceptionType ERROR_CANNOT_CREATE_FILE = new DynamicCommandExceptionType((code) -> {
        return new TranslationTextComponent("console.zetter.error.file_write_error", code);
    });

    static ArgumentBuilder<CommandSource, ?> register() {
        return Commands.literal("export")
            .requires(cs -> cs.hasPermission(2))
            .then(
                Commands.literal("server")
                    .then(
                        Commands.argument("painting", PaintingLookupArgument.painting())
                            .executes(ctx -> execute(
                                ctx.getSource(),
                                ctx.getSource().getPlayerOrException(),
                                ctx.getSource().getLevel(),
                                PaintingLookupArgument.getPaintingInput(ctx, "painting")
                            ))
                    )
            );
    }

    private static int execute(CommandSource source, ServerPlayerEntity player, World level, PaintingInput paintingInput) throws CommandException, CommandSyntaxException {
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
