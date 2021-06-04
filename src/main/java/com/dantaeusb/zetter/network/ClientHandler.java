package com.dantaeusb.zetter.network;

import com.dantaeusb.zetter.Zetter;
import com.dantaeusb.zetter.canvastracker.CanvasTrackerCapability;
import com.dantaeusb.zetter.canvastracker.ICanvasTracker;
import com.dantaeusb.zetter.container.ArtistTableContainer;
import com.dantaeusb.zetter.container.EaselContainer;
import com.dantaeusb.zetter.core.ModNetwork;
import com.dantaeusb.zetter.entity.item.CustomPaintingEntity;
import com.dantaeusb.zetter.network.packet.painting.SCanvasSyncMessage;
import com.dantaeusb.zetter.network.packet.painting.SSpawnCustomPaintingPacket;
import com.dantaeusb.zetter.storage.AbstractCanvasData;
import com.dantaeusb.zetter.storage.CanvasData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.item.PaintingEntity;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.LogicalSidedProvider;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.Optional;
import java.util.function.Supplier;

public class ClientHandler {
    /**
     * Handle sync packet on client
     *
     * @param packetIn
     * @param ctxSupplier
     */
    public static void handleSync(final SCanvasSyncMessage packetIn, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        LogicalSide sideReceived = ctx.getDirection().getReceptionSide();
        ctx.setPacketHandled(true);

        if (sideReceived != LogicalSide.CLIENT) {
            Zetter.LOG.warn("SCanvasSyncMessage received on wrong side:" + ctx.getDirection().getReceptionSide());
            return;
        }

        Optional<ClientWorld> clientWorld = LogicalSidedProvider.CLIENTWORLD.get(sideReceived);
        if (!clientWorld.isPresent()) {
            Zetter.LOG.warn("SCanvasSyncMessage context could not provide a ClientWorld.");
            return;
        }

        ctx.enqueueWork(() -> processSync(packetIn, clientWorld.get()));
    }

    public static void processSync(final SCanvasSyncMessage packetIn, ClientWorld world) {
        ClientPlayerEntity player = Minecraft.getInstance().player;
        AbstractCanvasData canvasData = packetIn.getCanvasData();

        if (player.openContainer instanceof EaselContainer) {
            // If it's the same canvas player is editing
            if (canvasData.getName().equals(((EaselContainer) player.openContainer).getCanvasData().getName())) {
                // Pushing changes that were added after sync packet was created
                // @todo: remove cast
                ((EaselContainer) player.openContainer).processSync((CanvasData) canvasData, packetIn.getTimestamp());
            }
        }

        if  (player.openContainer instanceof ArtistTableContainer) {
            // If player's combining canvases

            ((ArtistTableContainer) player.openContainer).updateCanvasCombination();
        }

        // Get overworld world instance
        ICanvasTracker canvasTracker = world.getCapability(CanvasTrackerCapability.CAPABILITY_CANVAS_TRACKER).orElse(null);

        if (canvasTracker == null) {
            Zetter.LOG.error("Cannot find world canvas capability");
            return;
        }

        canvasTracker.registerCanvasData(canvasData);
    }

    /**
     * Handle sync packet on client
     *
     * @param packetIn
     * @param ctxSupplier
     */
    /*public static void handleSpawnPainting(final SSpawnCustomPaintingPacket packetIn, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        LogicalSide sideReceived = ctx.getDirection().getReceptionSide();
        ctx.setPacketHandled(true);

        if (sideReceived != LogicalSide.CLIENT) {
            Zetter.LOG.warn("SSpawnCustomPaintingPacket received on wrong side:" + ctx.getDirection().getReceptionSide());
            return;
        }

        Optional<ClientWorld> clientWorld = LogicalSidedProvider.CLIENTWORLD.get(sideReceived);
        if (!clientWorld.isPresent()) {
            Zetter.LOG.warn("SSpawnCustomPaintingPacket context could not provide a ClientWorld.");
            return;
        }

        ctx.enqueueWork(() -> processSpawnPainting(packetIn, clientWorld.get()));
    }

    public static void processSpawnPainting(final SSpawnCustomPaintingPacket packetIn, ClientWorld world) {
        PaintingEntity paintingentity = new CustomPaintingEntity(world, packetIn.getPosition(), packetIn.getFacing(), packetIn.);
        paintingentity.setEntityId(packetIn.getEntityId());
        paintingentity.setUniqueId(packetIn.getUniqueId());

        world.addEntity(packetIn.getEntityId(), paintingentity);
    }*/

    public static boolean isThisProtocolAcceptedByClient(String protocolVersion) {
        return ModNetwork.MESSAGE_PROTOCOL_VERSION.equals(protocolVersion);
    }
}
