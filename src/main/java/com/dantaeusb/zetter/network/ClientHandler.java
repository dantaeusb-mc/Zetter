package com.dantaeusb.zetter.network;

import com.dantaeusb.zetter.Zetter;
import com.dantaeusb.zetter.canvastracker.CanvasTrackerCapability;
import com.dantaeusb.zetter.canvastracker.ICanvasTracker;
import com.dantaeusb.zetter.container.ArtistTableContainer;
import com.dantaeusb.zetter.container.EaselContainer;
import com.dantaeusb.zetter.core.Helper;
import com.dantaeusb.zetter.core.ModNetwork;
import com.dantaeusb.zetter.network.packet.SCanvasSyncMessage;
import com.dantaeusb.zetter.network.packet.SEaselCanvasChangePacket;
import com.dantaeusb.zetter.network.packet.SPaintingSyncMessage;
import com.dantaeusb.zetter.storage.AbstractCanvasData;
import com.dantaeusb.zetter.storage.CanvasData;
import com.dantaeusb.zetter.storage.PaintingData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.LogicalSidedProvider;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.Optional;
import java.util.function.Supplier;

public class ClientHandler {
    public static void processCanvasSync(final SCanvasSyncMessage packetIn, ClientWorld world) {
        ClientPlayerEntity player = Minecraft.getInstance().player;
        AbstractCanvasData canvasData = packetIn.getCanvasData();

        if (
                player.containerMenu instanceof EaselContainer
                && ((EaselContainer) player.containerMenu).isCanvasAvailable()
        ) {
            // If it's the same canvas player is editing
            if (canvasData.getId().equals(((EaselContainer) player.containerMenu).getCanvasData().getId())) {
                // Pushing changes that were added after sync packet was created
                // @todo: remove cast
                ((EaselContainer) player.containerMenu).processSync((CanvasData) canvasData, packetIn.getTimestamp());
            }
        }

        if  (player.containerMenu instanceof ArtistTableContainer) {
            // If player's combining canvases
            // @todo: not sure if needed

            ((ArtistTableContainer) player.containerMenu).updateCanvasCombination();
        }

        // Get overworld world instance
        ICanvasTracker canvasTracker = world.getCapability(CanvasTrackerCapability.CAPABILITY_CANVAS_TRACKER).orElse(null);

        if (canvasTracker == null) {
            Zetter.LOG.error("Cannot find world canvas capability");
            return;
        }

        canvasTracker.registerCanvasData(canvasData);
    }

    public static void processPaintingSync(final SPaintingSyncMessage packetIn, ClientWorld world) {
        PaintingData canvasData = packetIn.getPaintingData();

        ICanvasTracker canvasTracker = Helper.getWorldCanvasTracker(world);

        if (canvasTracker == null) {
            Zetter.LOG.error("Cannot find world canvas capability");
            return;
        }

        canvasTracker.registerCanvasData(canvasData);
    }

    public static void processEaselCanvasUpdate(final SEaselCanvasChangePacket packetIn, ClientWorld world) {
        ClientPlayerEntity player = Minecraft.getInstance().player;

        if (player.containerMenu instanceof EaselContainer) {
            ((EaselContainer) player.containerMenu).handleCanvasChange(packetIn.getItem());
        }
    }
}
