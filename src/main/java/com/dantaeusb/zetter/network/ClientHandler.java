package com.dantaeusb.zetter.network;

import com.dantaeusb.zetter.Zetter;
import com.dantaeusb.zetter.canvastracker.ICanvasTracker;
import com.dantaeusb.zetter.menu.ArtistTableMenu;
import com.dantaeusb.zetter.menu.EaselContainerMenu;
import com.dantaeusb.zetter.core.Helper;
import com.dantaeusb.zetter.core.ModCapabilities;
import com.dantaeusb.zetter.network.packet.SCanvasSyncMessage;
import com.dantaeusb.zetter.network.packet.SEaselCanvasChangePacket;
import com.dantaeusb.zetter.network.packet.SPaintingSyncMessage;
import com.dantaeusb.zetter.storage.AbstractCanvasData;
import com.dantaeusb.zetter.storage.CanvasData;
import com.dantaeusb.zetter.storage.PaintingData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.multiplayer.ClientLevel;

public class ClientHandler {
    public static void processCanvasSync(final SCanvasSyncMessage packetIn, ClientLevel world) {
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
                ((EaselContainerMenu) player.containerMenu).processSync(canvasCode, (CanvasData) canvasData, packetIn.getTimestamp());
            }
        }

        if  (player.containerMenu instanceof ArtistTableMenu) {
            // If player's combining canvases
            // @todo: not sure if needed

            ((ArtistTableMenu) player.containerMenu).updateCanvasCombination();
        }

        // Get overworld world instance
        ICanvasTracker canvasTracker = world.getCapability(ModCapabilities.CANVAS_TRACKER).orElse(null);

        if (canvasTracker == null) {
            Zetter.LOG.error("Cannot find world canvas capability");
            return;
        }

        canvasTracker.registerCanvasData(canvasCode, canvasData);
    }

    public static void processPaintingSync(final SPaintingSyncMessage packetIn, ClientLevel world) {
        String canvasCode = packetIn.getCanvasCode();
        PaintingData canvasData = packetIn.getPaintingData();

        ICanvasTracker canvasTracker = Helper.getWorldCanvasTracker(world);

        if (canvasTracker == null) {
            Zetter.LOG.error("Cannot find world canvas capability");
            return;
        }

        canvasTracker.registerCanvasData(canvasCode, canvasData);
    }

    public static void processEaselCanvasUpdate(final SEaselCanvasChangePacket packetIn, ClientLevel world) {
        LocalPlayer player = Minecraft.getInstance().player;

        if (player.containerMenu instanceof EaselContainerMenu) {
            ((EaselContainerMenu) player.containerMenu).handleCanvasChange(packetIn.getItem());
        }
    }
}
