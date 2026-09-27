package me.dantaeusb.zetter.core;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.capability.canvastracker.CanvasServerTracker;
import me.dantaeusb.zetter.client.gui.overlay.CanvasOverlay;
import me.dantaeusb.zetter.client.renderer.CanvasRenderer;
import me.dantaeusb.zetter.entity.item.AbstractBoardEntity;
import me.dantaeusb.zetter.item.crafting.CanvasCuttingRecipe;
import me.dantaeusb.zetter.item.crafting.CanvasStitchingRecipe;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.event.TickEvent;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

@Mod.EventBusSubscriber(modid = Zetter.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ZetterGameEvents {
    @SubscribeEvent
    public static void onPlayerDisconnected(PlayerEvent.PlayerLoggedOutEvent event) {
        Player player = event.getEntity();
        CanvasServerTracker canvasTracker = (CanvasServerTracker) Helper.getLevelCanvasTracker(player.level());

        canvasTracker.stopTrackingAllCanvases(player.getUUID());
    }

    /**
     * Cancel punch when working on a board with anything it takes, which is applied
     * as a tool instead
     *
     * @param event
     */
    @SubscribeEvent
    public static void onAttackBoard(AttackEntityEvent event) {
        if (!(event.getTarget() instanceof AbstractBoardEntity board)) {
            return;
        }

        for (InteractionHand hand : InteractionHand.values()) {
            if (board.acceptsImplement(event.getEntity().getItemInHand(hand))) {
                event.setCanceled(true);
                return;
            }
        }
    }

    @SubscribeEvent
    public static void tickCanvasTracker(TickEvent.ServerTickEvent event) {
        CanvasServerTracker canvasTracker = (CanvasServerTracker) Helper.getLevelCanvasTracker(ServerLifecycleHooks.getCurrentServer().overworld());
        canvasTracker.tick();
    }

    /**
     * @param event
     * @todo: [MED] Do we really need that hook here? It might be called very frequently
     */
    @SubscribeEvent
    public static void onRenderTickStart(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && Minecraft.getInstance().level != null) {
            CanvasRenderer.getInstance().update(Util.getMillis());
        }

        for (CanvasOverlay<?> overlay : ZetterOverlays.OVERLAYS.values()) {
            overlay.tick();
        }
    }

    /**
     * Fired only when the craft is confirmed, right before ingredients
     * are consumed, so they are still in the grid: creates the stitched
     * canvas or the top-left cut part on the result.
     * Client builds its own preview, see StitchedCanvasPreview.
     *
     * @param event
     */
    @SubscribeEvent
    public static void onItemCraftedEvent(PlayerContainerEvent.ItemCraftedEvent event) {
        Player player = event.getEntity();

        if (player == null || player.level().isClientSide()) {
            return;
        }

        if (event.getInventory() instanceof CraftingContainer craftingContainer) {
            player.level().getRecipeManager().getRecipeFor(
                RecipeType.CRAFTING,
                craftingContainer,
                player.level()
            ).ifPresent(recipe -> {
                if (recipe instanceof CanvasStitchingRecipe) {
                    CanvasStitchingHelper.finishStitching(craftingContainer, event.getCrafting(), player);
                } else if (recipe instanceof CanvasCuttingRecipe) {
                    CanvasCuttingHelper.finishCutting(craftingContainer, event.getCrafting(), player);
                }
            });
        }
    }
}
