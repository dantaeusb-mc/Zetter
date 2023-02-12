package me.dantaeusb.zetter.server.command;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.capability.canvastracker.CanvasServerTracker;
import me.dantaeusb.zetter.core.Helper;
import me.dantaeusb.zetter.core.ZetterCapabilities;
import me.dantaeusb.zetter.core.ZetterNetwork;
import me.dantaeusb.zetter.network.packet.SCanvasSyncExportErrorPacket;
import me.dantaeusb.zetter.network.packet.SCanvasSyncExportPacket;
import me.dantaeusb.zetter.storage.PaintingData;
import net.minecraft.command.CommandException;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.PacketDistributor;

/**
 * Need a lot of magic here
 */
public class ExportClientCommand {
    private static final DynamicCommandExceptionType ERROR_UNKNOWN = new DynamicCommandExceptionType((code) -> {
        return new TranslationTextComponent("console.zetter.error.unknown", code);
    });

    static ArgumentBuilder<CommandSource, ?> register() {
        return Commands.literal("export")
            .requires(cs -> cs.hasPermission(0))
            .then(
                Commands.literal("client")
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
        try {
            final CanvasServerTracker canvasTracker = (CanvasServerTracker) Helper.getLevelCanvasTracker(level);

            if (canvasTracker == null) {
                Zetter.LOG.error("Cannot find world canvas capability");

                SCanvasSyncExportErrorPacket canvasSyncExportErrorMessage = new SCanvasSyncExportErrorPacket("console.zetter.error.unknown", null);
                ZetterNetwork.simpleChannel.send(PacketDistributor.PLAYER.with(() -> player), canvasSyncExportErrorMessage);

                return 0;
            }

            if (!paintingInput.hasPaintingData(level)) {
                SCanvasSyncExportErrorPacket canvasSyncExportErrorMessage = new SCanvasSyncExportErrorPacket("console.zetter.error.painting_not_found", paintingInput.getPaintingCode());
                ZetterNetwork.simpleChannel.send(PacketDistributor.PLAYER.with(() -> player), canvasSyncExportErrorMessage);

                return 0;
            }

            SCanvasSyncExportPacket canvasSyncExportMessage = new SCanvasSyncExportPacket(paintingInput.getPaintingCode(), paintingInput.getPaintingData(), System.currentTimeMillis());
            ZetterNetwork.simpleChannel.send(PacketDistributor.PLAYER.with(() -> player), canvasSyncExportMessage);
        } catch (Exception e) {
            Zetter.LOG.error(e.getMessage());

            SCanvasSyncExportErrorPacket canvasSyncExportErrorMessage = new SCanvasSyncExportErrorPacket("console.zetter.error.unknown", null);
            ZetterNetwork.simpleChannel.send(PacketDistributor.PLAYER.with(() -> player), canvasSyncExportErrorMessage);

            throw e;
        }
    }
}
