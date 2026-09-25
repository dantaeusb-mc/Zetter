package me.dantaeusb.zetter.client.input;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.core.ZetterNetwork;
import me.dantaeusb.zetter.entity.item.AbstractBoardEntity;
import me.dantaeusb.zetter.item.BlackboardImplement;
import me.dantaeusb.zetter.network.packet.CCanvasHolderStopUsingPacket;
import me.dantaeusb.zetter.network.packet.CImplementUseCanvasHolderPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector2f;

import javax.annotation.Nullable;

/**
 * Working on a board in the world rather than through a screen, whichever kind of
 * board it is.
 */
@Mod.EventBusSubscriber(modid = Zetter.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class BlackboardHandler {
    /**
     * Board being worked on, or -1. Held rather than looked up every tick because
     * starting costs a packet, and because leaving one has to be noticed.
     */
    private static int boardId = -1;

    /**
     * Whether the previous tick drew, which is what tells the tool a stroke carried
     * on rather than started here — a swept tool fills the gap back to the last point
     */
    private static boolean continuing = false;

    @SubscribeEvent
    public static void onAttackKey(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isAttack()) {
            return;
        }

        final Minecraft minecraft = Minecraft.getInstance();
        final AbstractBoardEntity board = lookedAtBoard(minecraft);

        if (board == null || implementHand(minecraft.player, board) == null) {
            return;
        }

        event.setSwingHand(false);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        final Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player == null || minecraft.level == null || minecraft.screen != null) {
            stop(minecraft);
            return;
        }

        if (!minecraft.options.keyAttack.isDown()) {
            stop(minecraft);
            return;
        }

        final AbstractBoardEntity board = lookedAtBoard(minecraft);
        final InteractionHand hand = board == null ? null : implementHand(minecraft.player, board);

        if (board == null || hand == null) {
            stop(minecraft);
            return;
        }

        draw(minecraft, board, hand);
    }

    private static void draw(Minecraft minecraft, AbstractBoardEntity board, InteractionHand hand) {
        final LocalPlayer player = minecraft.player;
        final ItemStack implementStack = player.getItemInHand(hand);
        final BlackboardImplement implement = (BlackboardImplement) implementStack.getItem();

        if (board.getId() != boardId) {
            stop(minecraft);

            /*
             * The server is told, but nothing is waited for: it has no say in what
             * the board looks like on this client, and if it refuses, the actions
             * are dropped when they arrive rather than never having been drawn.
             */
            ZetterNetwork.simpleChannel.sendToServer(new CImplementUseCanvasHolderPacket(board.getId(), hand));
            board.addPlayerUsing(player, implementStack);

            boardId = board.getId();
            continuing = false;
        }

        final float partialTicks = minecraft.getFrameTime();
        final Vector2f pixel = board.getCanvasPixel(
            player.getEyePosition(partialTicks),
            player.getViewVector(partialTicks),
            partialTicks
        );

        // Under the frame is not part of the slate
        if (pixel == null || !board.isDrawablePixel(pixel.x, pixel.y)) {
            return;
        }

        board.getCanvasState().useTool(
            player, implement.getTool(),
            pixel.x, pixel.y,
            implement.getToolColor(implementStack),
            implement.getToolParameters(implementStack),
            continuing
        );

        continuing = true;
    }

    /**
     * Let go of whatever board was being worked on. Safe to call when there is none,
     * which is most ticks.
     */
    private static void stop(Minecraft minecraft) {
        if (boardId == -1) {
            return;
        }

        if (minecraft.level != null && minecraft.player != null) {
            final Entity previous = minecraft.level.getEntity(boardId);

            if (previous instanceof AbstractBoardEntity board) {
                board.removePlayerUsing(minecraft.player);
            }

            ZetterNetwork.simpleChannel.sendToServer(new CCanvasHolderStopUsingPacket(boardId));
        }

        boardId = -1;
        continuing = false;
    }

    /**
     * Hand holding something this board can be worked on with, main first, or null
     * for neither. The board decides, so the same rule applies here and on the server.
     *
     * @param player
     * @param board
     * @return
     */
    private static @Nullable InteractionHand implementHand(@Nullable LocalPlayer player, AbstractBoardEntity board) {
        if (player == null) {
            return null;
        }

        for (InteractionHand hand : InteractionHand.values()) {
            if (board.acceptsImplement(player.getItemInHand(hand))) {
                return hand;
            }
        }

        return null;
    }

    /**
     * Board under the crosshair
     *
     * @param minecraft
     * @return
     */
    private static @Nullable AbstractBoardEntity lookedAtBoard(Minecraft minecraft) {
        final HitResult hitResult = minecraft.hitResult;

        if (hitResult == null || hitResult.getType() != HitResult.Type.ENTITY) {
            return null;
        }

        final Entity entity = ((EntityHitResult) hitResult).getEntity();

        return entity instanceof AbstractBoardEntity board ? board : null;
    }
}
