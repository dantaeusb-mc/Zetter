package me.dantaeusb.zetter.server.command;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import me.dantaeusb.zetter.core.ZetterItems;
import me.dantaeusb.zetter.item.PaintingItem;
import me.dantaeusb.zetter.storage.PaintingData;
import net.minecraft.commands.CommandRuntimeException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class RestoreCommand {
    private static final DynamicCommandExceptionType ERROR_PAINTING_NOT_FOUND = new DynamicCommandExceptionType((code) -> {
        return Component.translatable("console.zetter.error.painting_not_found", code);
    });

    static ArgumentBuilder<CommandSourceStack, ?> register() {
        return Commands.literal("restore")
            .requires(cs -> cs.hasPermission(Commands.LEVEL_GAMEMASTERS))
            .then(
                Commands.argument("painting", PaintingLookupArgument.painting())
                    .executes(ctx -> execute(
                        ctx.getSource(),
                        ctx.getSource().getPlayer(),
                        ctx.getSource().getLevel(),
                        PaintingLookupArgument.getPaintingInput(ctx, "painting")
                    ))
            );
    }

    private static int execute(CommandSourceStack source, Player player, Level level, PaintingInput paintingInput) throws CommandRuntimeException, CommandSyntaxException {
        ItemStack paintingItem = new ItemStack(ZetterItems.PAINTING.get());

        if (!paintingInput.hasPaintingData(level)) {
            throw ERROR_PAINTING_NOT_FOUND.create(paintingInput.getPaintingCode());
        }

        String paintingCode = paintingInput.getPaintingCode();
        PaintingData paintingData = paintingInput.getPaintingData();

        PaintingItem.storePaintingData(paintingItem, paintingCode, paintingData, 1);

        boolean flag = player.getInventory().add(paintingItem);
        if (flag && paintingItem.isEmpty()) {
            paintingItem.setCount(1);
            ItemEntity itemEntity = player.drop(paintingItem, false);
            if (itemEntity != null) {
                itemEntity.makeFakeItem();
            }

            player.level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2F, ((player.getRandom().nextFloat() - player.getRandom().nextFloat()) * 0.7F + 1.0F) * 2.0F);
            player.containerMenu.broadcastChanges();
        } else {
            ItemEntity itemEntity = player.drop(paintingItem, false);

            if (itemEntity != null) {
                itemEntity.setNoPickUpDelay();
                itemEntity.setOwner(player.getUUID());
            }
        }

        source.sendSuccess(Component.translatable("commands.give.success.single", 1, paintingItem.getDisplayName(), player.getDisplayName()), true);

        return 1;
    }
}
