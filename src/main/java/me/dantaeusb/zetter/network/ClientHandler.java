package me.dantaeusb.zetter.network;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.canvastracker.ICanvasTracker;
import me.dantaeusb.zetter.entity.item.EaselEntity;
import me.dantaeusb.zetter.menu.ArtistTableMenu;
import me.dantaeusb.zetter.menu.EaselContainerMenu;
import me.dantaeusb.zetter.core.Helper;
import me.dantaeusb.zetter.core.ZetterCapabilities;
import me.dantaeusb.zetter.network.packet.SCanvasSyncMessage;
import me.dantaeusb.zetter.network.packet.SEaselCanvasChangePacket;
import me.dantaeusb.zetter.network.packet.SPaintingSyncMessage;
import me.dantaeusb.zetter.storage.AbstractCanvasData;
import me.dantaeusb.zetter.storage.CanvasData;
import me.dantaeusb.zetter.storage.PaintingData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

public class ClientHandler {
    public static void processCanvasSync(final SCanvasSyncMessage packetIn, Level world) {
        final LocalPlayer player = Minecraft.getInstance().player;
        final String canvasCode = packetIn.getCanvasCode();
        final AbstractCanvasData canvasData = packetIn.getCanvasData();

        if (
                player.containerMenu instanceof EaselContainerMenu
                && ((EaselContainerMenu) player.containerMenu).isCanvasAvailable()
        ) {
            // If it's the same canvas player is editing
            if (canvasCode.equals(((EaselContainerMenu) player.containerMenu).getCanvasCode())) {
                // Pushing changes that were added after sync packet was created
                // @todo: remove cast
                ((EaselContainerMenu) player.containerMenu).handleCanvasSync(canvasCode, (CanvasData) canvasData, packetIn.getTimestamp());
            }
        }

        if  (player.containerMenu instanceof ArtistTableMenu) {
            // If player's combining canvases
            // @todo: not sure if needed

            ((ArtistTableMenu) player.containerMenu).updateCanvasCombination();
        }

        // Get overworld world instance
        ICanvasTracker canvasTracker = world.getCapability(ZetterCapabilities.CANVAS_TRACKER).orElse(null);

        if (canvasTracker == null) {
            Zetter.LOG.error("Cannot find world canvas capability");
            return;
        }

        canvasTracker.registerCanvasData(canvasCode, canvasData);
    }

    public static void processPaintingSync(final SPaintingSyncMessage packetIn, Level world) {
        String canvasCode = packetIn.getCanvasCode();
        PaintingData canvasData = packetIn.getPaintingData();

        ICanvasTracker canvasTracker = Helper.getWorldCanvasTracker(world);

        if (canvasTracker == null) {
            Zetter.LOG.error("Cannot find world canvas capability");
            return;
        }

        canvasTracker.registerCanvasData(canvasCode, canvasData);
    }

    public static void processEaselCanvasUpdate(final SEaselCanvasChangePacket packetIn, Level world) {
        LocalPlayer player = Minecraft.getInstance().player;
        Entity easel = world.getEntity(packetIn.getEntityId());

        if (world.getEntity(packetIn.getEntityId()) instanceof EaselEntity) {
            ((EaselEntity) easel).putCanvasStack(packetIn.getItem());
        }

        if (player.containerMenu instanceof EaselContainerMenu) {
        }
    }
}
