package me.dantaeusb.zetter.server.command;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import me.dantaeusb.zetter.core.ZetterItems;
import me.dantaeusb.zetter.item.PaintingItem;
import me.dantaeusb.zetter.storage.PaintingData;
import net.minecraft.client.audio.SoundSource;
import net.minecraft.command.CommandException;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;

public class RestoreCommand {
    private static final DynamicCommandExceptionType ERROR_PAINTING_NOT_FOUND = new DynamicCommandExceptionType((code) -> {
        return new TranslationTextComponent("console.zetter.error.painting_not_found", code);
    });

    static ArgumentBuilder<CommandSource, ?> register() {
        return Commands.literal("restore")
            .requires(cs -> cs.hasPermission(2))
            .then(
                Commands.argument("painting", PaintingLookupArgument.painting())
                    .executes(ctx -> execute(
                        ctx.getSource(),
                        (PlayerEntity) ctx.getSource().getEntity(),
                        ctx.getSource().getLevel(),
                        PaintingLookupArgument.getPaintingInput(ctx, "painting")
                    ))
            );
    }

    private static int execute(CommandSource source, PlayerEntity player, World level, PaintingInput paintingInput) throws CommandException, CommandSyntaxException {
        ItemStack paintingItem = new ItemStack(ZetterItems.PAINTING.get());

        if (!paintingInput.hasPaintingData(level)) {
            throw ERROR_PAINTING_NOT_FOUND.create(paintingInput.getPaintingCode());
        }

        String paintingCode = paintingInput.getPaintingCode();
        PaintingData paintingData = paintingInput.getPaintingData();

        PaintingItem.storePaintingData(paintingItem, paintingCode, paintingData, 1);

        boolean flag = player.inventory.add(paintingItem);
        if (flag && paintingItem.isEmpty()) {
            paintingItem.setCount(1);
            ItemEntity itemEntity = player.drop(paintingItem, false);
            if (itemEntity != null) {
                itemEntity.makeFakeItem();
            }

            player.level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_PICKUP, SoundCategory.PLAYERS, 0.2F, ((player.getRandom().nextFloat() - player.getRandom().nextFloat()) * 0.7F + 1.0F) * 2.0F);
            player.containerMenu.broadcastChanges();
        } else {
            ItemEntity itemEntity = player.drop(paintingItem, false);

            if (itemEntity != null) {
                itemEntity.setNoPickUpDelay();
                itemEntity.setOwner(player.getUUID());
            }
        }

        source.sendSuccess(new TranslationTextComponent("commands.give.success.single", 1, paintingItem.getDisplayName(), player.getDisplayName()), true);

        return 1;
    }
}
