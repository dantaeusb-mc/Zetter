package me.dantaeusb.zetter.client.input;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.entity.item.BlackboardEntity;
import me.dantaeusb.zetter.item.ChalkItem;
import me.dantaeusb.zetter.core.ZetterNetwork;
import me.dantaeusb.zetter.network.packet.CCanvasHolderStopUsingPacket;
import me.dantaeusb.zetter.network.packet.CChalkUseCanvasHolderPacket;
import me.dantaeusb.zetter.painting.Tool;
import me.dantaeusb.zetter.painting.parameters.BrushParameters;
import me.dantaeusb.zetter.painting.pipes.BlendingPipe;
import me.dantaeusb.zetter.painting.pipes.DitheringPipe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector2f;

import javax.annotation.Nullable;

/**
 * Drawing on a board with chalk, which happens in the world rather than in a screen
 */
@Mod.EventBusSubscriber(modid = Zetter.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ChalkHandler {
    private static final BrushParameters CHALK = new BrushParameters(
        BrushParameters.MIN_SIZE, 1.0f,
        BlendingPipe.BlendingOption.SUBTRACTIVE,
        DitheringPipe.DitheringOption.NO_DITHERING
    );

    /**
     * Board being drawn on, or -1. Held rather than looked up every tick because
     * starting costs a packet, and because leaving one has to be noticed.
     */
    private static int boardId = -1;

    /**
     * Whether the previous tick drew, which is what tells the tool a stroke carried
     * on rather than started here — a brush fills the gap back to the last point
     */
    private static boolean continuing = false;

    @SubscribeEvent
    public static void onAttackKey(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isAttack()) {
            return;
        }

        final Minecraft minecraft = Minecraft.getInstance();

        if (chalkHand(minecraft) == null || lookedAtBoard(minecraft) == null) {
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

        final InteractionHand hand = chalkHand(minecraft);
        final BlackboardEntity board = lookedAtBoard(minecraft);

        if (hand == null || board == null) {
            stop(minecraft);
            return;
        }

        draw(minecraft, board, hand);
    }

    private static void draw(Minecraft minecraft, BlackboardEntity board, InteractionHand hand) {
        final LocalPlayer player = minecraft.player;
        final ItemStack chalkStack = player.getItemInHand(hand);

        if (board.getId() != boardId) {
            stop(minecraft);

            /*
             * The server is told, but nothing is waited for: it has no say in what
             * the board looks like on this client, and if it refuses, the actions
             * are dropped when they arrive rather than never having been drawn.
             */
            ZetterNetwork.simpleChannel.sendToServer(new CChalkUseCanvasHolderPacket(board.getId(), hand));
            board.addPlayerUsing(player, chalkStack);

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
            player, Tool.BRUSH,
            pixel.x, pixel.y,
            ((ChalkItem) chalkStack.getItem()).getColor(),
            CHALK,
            continuing
        );

        continuing = true;
    }

    /**
     * Let go of whatever board was being drawn on. Safe to call when there is none,
     * which is most ticks.
     */
    private static void stop(Minecraft minecraft) {
        if (boardId == -1) {
            return;
        }

        if (minecraft.level != null && minecraft.player != null) {
            final Entity previous = minecraft.level.getEntity(boardId);

            if (previous instanceof BlackboardEntity board) {
                board.removePlayerUsing(minecraft.player);
            }

            ZetterNetwork.simpleChannel.sendToServer(new CCanvasHolderStopUsingPacket(boardId));
        }

        boardId = -1;
        continuing = false;
    }

    /**
     * Hand holding chalk, main first, or null for neither
     */
    private static @Nullable InteractionHand chalkHand(Minecraft minecraft) {
        if (minecraft.player == null) {
            return null;
        }

        for (InteractionHand hand : InteractionHand.values()) {
            if (minecraft.player.getItemInHand(hand).getItem() instanceof ChalkItem) {
                return hand;
            }
        }

        return null;
    }

    /**
     * Board under the crosshair.
     */
    private static @Nullable BlackboardEntity lookedAtBoard(Minecraft minecraft) {
        final HitResult hitResult = minecraft.hitResult;

        if (hitResult == null || hitResult.getType() != HitResult.Type.ENTITY) {
            return null;
        }

        final Entity entity = ((EntityHitResult) hitResult).getEntity();

        return entity instanceof BlackboardEntity board ? board : null;
    }
}
