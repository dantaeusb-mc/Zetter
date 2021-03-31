package com.dantaeusb.zetter.network;

import com.dantaeusb.zetter.Zetter;
import com.dantaeusb.zetter.canvastracker.CanvasTrackerCapability;
import com.dantaeusb.zetter.canvastracker.ICanvasTracker;
import com.dantaeusb.zetter.container.EaselContainer;
import com.dantaeusb.zetter.core.ModNetwork;
import com.dantaeusb.zetter.network.packet.painting.SCanvasSyncMessage;
import com.dantaeusb.zetter.storage.CanvasData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.LogicalSidedProvider;
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
        CanvasData canvasData = packetIn.getCanvasData();

        if (player.openContainer instanceof EaselContainer) {
            // If it's the same canvas player is editing
            if (canvasData.getName().equals(((EaselContainer) player.openContainer).getCanvasData().getName())) {
                // Pushing changes that were added after sync packet was created
                ((EaselContainer) player.openContainer).processSync(canvasData, packetIn.getTimestamp());
            }
        }

        // Get overworld world instance
        ICanvasTracker canvasTracker = world.getCapability(CanvasTrackerCapability.CAPABILITY_CANVAS_TRACKER).orElse(null);

        if (canvasTracker == null) {
            Zetter.LOG.error("Cannot find world canvas capability");
            return;
        }

        canvasTracker.registerCanvasData(canvasData);
    }

    public static boolean isThisProtocolAcceptedByClient(String protocolVersion) {
        return ModNetwork.MESSAGE_PROTOCOL_VERSION.equals(protocolVersion);
    }
}
