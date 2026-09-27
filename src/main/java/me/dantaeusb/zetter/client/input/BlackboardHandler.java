package me.dantaeusb.zetter.client.input;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.core.ZetterNetwork;
import me.dantaeusb.zetter.core.ZetterSounds;
import me.dantaeusb.zetter.entity.item.AbstractBoardEntity;
import me.dantaeusb.zetter.client.sound.ChalkScratchSound;
import me.dantaeusb.zetter.item.BlackboardImplement;
import me.dantaeusb.zetter.item.ChalkItem;
import me.dantaeusb.zetter.storage.CanvasData;
import me.dantaeusb.zetter.network.packet.CCanvasHolderStopUsingPacket;
import me.dantaeusb.zetter.network.packet.CImplementUseCanvasHolderPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.util.RandomSource;
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

    /**
     * The scratching, while there is any. Held rather than looked up, since the sound
     * engine is told how far the hand moved and has to be told by someone.
     */
    private static ChalkScratchSound scratch = null;

    /**
     * Where the last tick drew, so this one knows how far the hand came. Speed is the
     * whole of the sound's expression, and nothing else measures it.
     */
    private static Vector2f lastPixel = null;

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

        scratch(minecraft, board, implementStack, pixel);

        continuing = true;
        lastPixel = pixel;
    }

    /**
     * The chalk landing on the board.
     *
     * Played at the listener like the scratching, since drawing means standing right
     * in front of the board and there is nothing for direction to say.
     *
     * @param minecraft
     */
    private static void touch(Minecraft minecraft) {
        final RandomSource random = minecraft.level.getRandom();

        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(
            ZetterSounds.CHALK_IMPACT.get(),
            0.85f + random.nextFloat() * 0.3f,
            0.6f + random.nextFloat() * 0.2f
        ));
    }

    /**
     * Keeps the scratching going, and tells it how fast the hand is moving.
     *
     * @param minecraft
     * @param board
     * @param implementStack
     * @param pixel
     */
    private static void scratch(Minecraft minecraft, AbstractBoardEntity board, ItemStack implementStack, Vector2f pixel) {
        if (!(implementStack.getItem() instanceof ChalkItem)) {
            return;
        }

        boolean touching = false;

        if (scratch == null || scratch.isStopped() || scratch.getBoard() != board) {
            scratch = new ChalkScratchSound(board);
            minecraft.getSoundManager().play(scratch);

            touching = true;
        }

        if (scratch.resume() || touching) {
            touch(minecraft);
        }

        if (lastPixel == null || !continuing) {
            return;
        }

        final float dx = pixel.x - lastPixel.x;
        final float dy = pixel.y - lastPixel.y;

        scratch.moved((float) Math.sqrt(dx * dx + dy * dy));

        /*
         * Where on the slate the chalk is, as a fraction either way. The board's modes
         * can only be excited away from their own nodes, so this is what decides which
         * of them answer — and why drawing a circle sounds different all the way round.
         */
        final CanvasData canvasData = board.getCanvasData();

        if (canvasData != null) {
            scratch.contact(pixel.x / canvasData.getWidth(), pixel.y / canvasData.getHeight());
        }
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

        // Released, not discarded: it fades itself out and waits to be picked up again
        if (scratch != null) {
            scratch.release();
        }

        boardId = -1;
        continuing = false;
        lastPixel = null;
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
