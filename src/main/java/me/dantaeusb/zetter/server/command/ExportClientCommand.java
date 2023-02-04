package me.dantaeusb.zetter.server.command;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import me.dantaeusb.zetter.core.ZetterNetwork;
import me.dantaeusb.zetter.network.packet.CCanvasRequestExportPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandRuntimeException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ITextComponent;

/**
 * Need a lot of magic here
 */
public class ExportClientCommand {
    private static final DynamicCommandExceptionType ERROR_UNKNOWN = new DynamicCommandExceptionType((code) -> {
        return new TranslationTextComponent("console.zetter.error.unknown", code);
    });

    static ArgumentBuilder<CommandSourceStack, ?> register() {
        return Commands.literal("export")
            .requires(cs -> cs.hasPermission(Commands.LEVEL_ALL))
            .then(
                Commands.literal("client")
                    .then(
                        Commands.argument("painting", PaintingLookupArgument.painting())
                            .executes(ctx -> execute(
                                ctx.getSource(),
                                PaintingLookupArgument.getPaintingInput(ctx, "painting")
                            ))
                    )
            );
    }

    private static int execute(CommandSourceStack source, PaintingInput paintingInput) throws CommandRuntimeException, CommandSyntaxException {
        CCanvasRequestExportPacket requestExportPacket = new CCanvasRequestExportPacket(paintingInput.getPaintingCode(), paintingInput.getPaintingTitle());

        try {
            ZetterNetwork.simpleChannel.sendToServer(requestExportPacket);

            String input = paintingInput.getPaintingCode() != null ? paintingInput.getPaintingCode() : paintingInput.getPaintingTitle();
            Minecraft.getInstance().getChatListener().handleSystemMessage(
                new TranslationTextComponent("console.zetter.result.requested_painting", input),
                false
            );
        } catch (Exception e) {
            throw ERROR_UNKNOWN.create(e.getMessage());
        }

        return 1;
    }
}
