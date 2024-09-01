package me.dantaeusb.zetter.core;

import com.mojang.datafixers.util.Either;
import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.client.gui.overlay.CanvasOverlay;
import me.dantaeusb.zetter.client.gui.overlay.PaintingInfoOverlay;
import me.dantaeusb.zetter.client.gui.tooltip.CanvasTooltipRenderer;
import me.dantaeusb.zetter.event.CanvasOverlayViewEvent;
import me.dantaeusb.zetter.event.CanvasRegisterEvent;
import me.dantaeusb.zetter.event.CanvasViewEvent;
import me.dantaeusb.zetter.item.FrameItem;
import me.dantaeusb.zetter.menu.ArtistTableMenu;
import me.dantaeusb.zetter.menu.EaselMenu;
import me.dantaeusb.zetter.storage.AbstractCanvasData;
import me.dantaeusb.zetter.storage.CanvasData;
import me.dantaeusb.zetter.storage.PaintingData;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegistryObject;

@Mod.EventBusSubscriber(modid = Zetter.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ZetterClientModEvents {
    /**
     * Handle event when canvas is viewed. Because canvas types are
     * extendable, we
     * @param event
     */
    @SubscribeEvent
    public static void onCanvasViewed(CanvasViewEvent event) {
        if (event.canvasData instanceof CanvasData canvasCanvasData) {
            ClientHelper.openCanvasScreen(event.player, event.canvasCode, canvasCanvasData, event.hand);
            event.setCanceled(true);
        } else if (event.canvasData instanceof PaintingData paintingData) {
            ClientHelper.openPaintingScreen(event.player, event.canvasCode, paintingData, event.hand);
            event.setCanceled(true);
        }
    }

    /**
     * Handle canvas registration on event, some menus/screens
     * might need to update
     *
     * We check if player is using some menus and try to update
     * canvas through those menus.
     *
     * @param event
     */
    @SubscribeEvent
    public static void onCanvasPreRegistered(CanvasRegisterEvent.Pre event) {
        if (!event.level.isClientSide()) {
            return;
        }

        String canvasCode = event.canvasCode;
        AbstractCanvasData canvasData = event.canvasData;
        long timestamp = event.timestamp;

        Player player = Minecraft.getInstance().player;

        if (canvasData instanceof CanvasData canvasCanvasData) {
            // Initialize canvas if client had no canvas loaded when it was updated
            if (player.containerMenu instanceof EaselMenu easelMenu) {
                String canvasItemCode = easelMenu.getCanvasItemCode();
                // If it's the same canvas player is editing
                if (canvasItemCode != null && canvasItemCode.equals(canvasCode)) {
                    // Pushing changes that were added after sync packet was created
                    if (easelMenu.handleCanvasSync(canvasCode, canvasCanvasData, timestamp)) {
                        // Easel does own registration with latest changes applied
                        event.setCanceled(true);
                    }
                }
            }
        }
    }

    /**
     * Handle canvas registration on event, some menus/screens
     * might need to update
     *
     * We check if player is using some menus and try to update
     * canvas through those menus.
     *
     * @param event
     */
    @SubscribeEvent
    public static void onCanvasPostRegistered(CanvasRegisterEvent.Post event) {
        if (!event.level.isClientSide()) {
            return;
        }

        String canvasCode = event.canvasCode;
        AbstractCanvasData canvasData = event.canvasData;
        long timestamp = event.timestamp;

        Player player = Minecraft.getInstance().player;

        if (canvasData instanceof CanvasData canvasCanvasData) {
            if  (player.containerMenu instanceof ArtistTableMenu artistTableMenu) {
                if (artistTableMenu.handleCanvasSync(canvasCode, canvasCanvasData, timestamp)) {
                    event.setCanceled(true);
                }
            }
        }
    }

    /**
     * Every other overlay should do that too, so they
     * won't overlap
     * @param event
     */
    @SubscribeEvent
    public static void overlayViewEvent(CanvasOverlayViewEvent<?> event) {
        if (event.canvasData instanceof PaintingData && ZetterOverlays.OVERLAYS.containsKey(PaintingData.OVERLAY_KEY)) {
            CanvasOverlay<?> overlay = ZetterOverlays.OVERLAYS.get(PaintingData.OVERLAY_KEY);

            if (overlay instanceof PaintingInfoOverlay) {
                ((PaintingInfoOverlay) overlay).setCanvasData((PaintingData) event.canvasData);
            }
        }
    }

    @SubscribeEvent
    public static void registerGatherTooltipComponent(RenderTooltipEvent.GatherComponents event) {
        if (event.getItemStack().is(ZetterItems.PAINTING.get())) {
            event.getTooltipElements().add(0, Either.right(new CanvasTooltipRenderer.CanvasComponent(event.getItemStack())));
        }

        if (event.getItemStack().is(ZetterItems.CANVAS.get())) {
            event.getTooltipElements().add(0, Either.right(new CanvasTooltipRenderer.CanvasComponent(event.getItemStack())));
        }

        for (RegistryObject<FrameItem> frame : ZetterItems.FRAMES.values()) {
            if (event.getItemStack().getItem() == frame.get() && FrameItem.getPaintingCode(event.getItemStack()) != null) {
                event.getTooltipElements().add(0, Either.right(new CanvasTooltipRenderer.CanvasComponent(event.getItemStack())));
            }
        }
    }
}
