package me.dantaeusb.zetter.server.command;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.core.ZetterItems;
import me.dantaeusb.zetter.item.PaintingItem;
import me.dantaeusb.zetter.storage.PaintingData;
import net.minecraft.commands.CommandRuntimeException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.io.File;
import java.io.IOException;

/**
 * Need a lot of magic here
 */
public class ExportCommand {
    private static final DynamicCommandExceptionType ERROR_PAINTING_NOT_FOUND = new DynamicCommandExceptionType((code) -> {
        return new TranslatableComponent("console.zetter.error.painting_not_found", code);
    });

    private static final DynamicCommandExceptionType ERROR_CANNOT_CREATE_FILE = new DynamicCommandExceptionType((code) -> {
        return new TranslatableComponent("console.zetter.error.file_write_error", code);
    });

    static ArgumentBuilder<CommandSourceStack, ?> register() {
        return Commands.literal("export")
            .requires(cs -> cs.hasPermission(Commands.LEVEL_ALL))
            .then(Commands.argument("painting", PaintingLookupArgument.painting()))
            .executes(ctx -> execute(ctx.getSource(), (Player) ctx.getSource().getEntity(), ctx.getSource().getLevel(), PaintingLookupArgument.getPaintingInput(ctx, "painting")));
    }

    private static int execute(CommandSourceStack source, Player player, Level level, PaintingInput paintingInput) throws CommandRuntimeException, CommandSyntaxException {
        if (!paintingInput.hasPaintingData(level)) {
            throw ERROR_PAINTING_NOT_FOUND.create(paintingInput.getPaintingCode());
        }


        return 1;
    }
}
