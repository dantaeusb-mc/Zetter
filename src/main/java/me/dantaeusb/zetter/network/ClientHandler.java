package me.dantaeusb.zetter.network;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.canvastracker.ICanvasTracker;
import me.dantaeusb.zetter.entity.item.EaselEntity;
import me.dantaeusb.zetter.menu.ArtistTableMenu;
import me.dantaeusb.zetter.menu.EaselContainerMenu;
import me.dantaeusb.zetter.core.Helper;
import me.dantaeusb.zetter.core.ZetterCapabilities;
import me.dantaeusb.zetter.network.packet.SCanvasSyncMessage;
import me.dantaeusb.zetter.network.packet.SCanvasSnapshotSync;
import me.dantaeusb.zetter.network.packet.SPaintingSyncMessage;
import me.dantaeusb.zetter.storage.CanvasData;
import me.dantaeusb.zetter.storage.PaintingData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.level.Level;

public class ClientHandler {
    public static void processCanvasSync(final SCanvasSyncMessage packetIn, Level world) {
        final LocalPlayer player = Minecraft.getInstance().player;
        final String canvasCode = packetIn.getCanvasCode();
        final CanvasData canvasData = (CanvasData) packetIn.getCanvasData();
        final long timestamp = packetIn.getTimestamp();

        // Get overworld world instance
        ICanvasTracker canvasTracker = world.getCapability(ZetterCapabilities.CANVAS_TRACKER).orElse(null);

        if (canvasTracker == null) {
            Zetter.LOG.error("Cannot find world canvas capability");
            return;
        }

        canvasTracker.registerCanvasData(canvasCode, canvasData);

        // Do extra things

        // Initialize canvas if client had no canvas loaded when it was updated
        if (player.containerMenu instanceof EaselContainerMenu) {
            String canvasItemCode = ((EaselContainerMenu) player.containerMenu).getCanvasItemCode();
            // If it's the same canvas player is editing
            if (canvasItemCode != null && canvasItemCode.equals(canvasCode)) {
                // Pushing changes that were added after sync packet was created
                ((EaselContainerMenu) player.containerMenu).handleCanvasSync(canvasCode, canvasData, timestamp);
            }
        }

        if  (player.containerMenu instanceof ArtistTableMenu) {
            // If player's combining canvases
            // @todo: not sure if needed

            ((ArtistTableMenu) player.containerMenu).updateCanvasCombination();
        }
    }

    public static void processPaintingDataSync(final SPaintingSyncMessage packetIn, Level world) {
        final LocalPlayer player = Minecraft.getInstance().player;

        String canvasCode = packetIn.getCanvasCode();
        PaintingData canvasData = packetIn.getPaintingData();

        ICanvasTracker canvasTracker = Helper.getWorldCanvasTracker(world);

        if (canvasTracker == null) {
            Zetter.LOG.error("Cannot find world canvas capability");
            return;
        }

        canvasTracker.registerCanvasData(canvasCode, canvasData);
    }

    public static void processSnapshotSync(final SCanvasSnapshotSync packetIn, Level world) {
        EaselEntity easel = (EaselEntity) world.getEntity(packetIn.getEaselEntityId());

        if (easel != null) {
            easel.getStateHandler().processSnapshotSyncClient(packetIn.getCanvasCode(), packetIn.getCanvasData(), packetIn.getTimestamp());
        } else {
            Zetter.LOG.warn("Unable to find entity " + packetIn.getEaselEntityId() + " disregarding canvas snapshot");
        }
    }
}
