package me.dantaeusb.zetter.core;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.capability.canvastracker.CanvasServerTracker;
import me.dantaeusb.zetter.client.gui.overlay.CanvasOverlay;
import me.dantaeusb.zetter.client.renderer.CanvasRenderer;
import me.dantaeusb.zetter.item.crafting.CanvasStitchingRecipe;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.event.TickEvent;
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
     * Put actual canvas data to the stitched canvas,
     * on client it would be enough to use COMBINED_CANVAS_CODE, but for the
     * safety let the client request and load the contents by regular means.
     *
     * @param event
     */
    @SubscribeEvent
    public static void onItemCraftedEvent(PlayerContainerEvent.ItemCraftedEvent event) {
        Player player = event.getEntity();

        if (player == null || player.level().isClientSide()) {
            return;
        }

        if (event.getCrafting().is(ZetterItems.CANVAS.get()) && event.getInventory() instanceof CraftingContainer craftingContainer) {
            player.level().getRecipeManager().getRecipeFor(
                RecipeType.CRAFTING,
                craftingContainer,
                player.level()
            ).ifPresent(recipe -> {
                if (!player.level().isClientSide && recipe instanceof CanvasStitchingRecipe) {
                    CanvasStitchingHelper.createStitchedCanvasAndWriteNewCanvasData(craftingContainer, event.getCrafting(), player);
                }
            });
        }
    }
}
