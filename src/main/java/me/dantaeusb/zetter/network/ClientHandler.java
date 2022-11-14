package me.dantaeusb.zetter.network;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.canvastracker.ICanvasTracker;
import me.dantaeusb.zetter.entity.item.EaselEntity;
import me.dantaeusb.zetter.event.CanvasViewEvent;
import me.dantaeusb.zetter.core.ZetterCapabilities;
import me.dantaeusb.zetter.network.packet.SCanvasSyncMessage;
import me.dantaeusb.zetter.network.packet.SEaselStateSync;
import me.dantaeusb.zetter.network.packet.SCanvasSyncViewMessage;
import me.dantaeusb.zetter.storage.AbstractCanvasData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;

public class ClientHandler {
    /**
     * When canvas sent from sever, update client's canvas
     * and process update on container screens
     *
     * @param packetIn
     * @param world
     */
    public static void processCanvasSync(final SCanvasSyncMessage packetIn, Level world) {
        final String canvasCode = packetIn.getCanvasCode();

        final AbstractCanvasData canvasData = packetIn.getCanvasData();
        final long timestamp = packetIn.getTimestamp();

        ICanvasTracker canvasTracker = world.getCapability(ZetterCapabilities.CANVAS_TRACKER)
                .orElseThrow(() -> new RuntimeException("Cannot find world canvas capability"));

        canvasTracker.registerCanvasData(canvasCode, canvasData, timestamp);
    }

    /**
     * Process SCanvasSyncViewMessage, open screen depending on
     * the type of canvas (basic or painting)
     * @param packetIn
     * @param world
     */
    public static void processCanvasSyncView(final SCanvasSyncViewMessage packetIn, Level world) {
        final LocalPlayer player = Minecraft.getInstance().player;
        final String canvasCode = packetIn.getCanvasCode();

        final AbstractCanvasData canvasData = packetIn.getCanvasData();

        CanvasViewEvent event = new CanvasViewEvent(player, canvasCode, canvasData, packetIn.getHand());

        MinecraftForge.EVENT_BUS.post(event);

        processCanvasSync(packetIn, world);
    }

    public static void processEaselStateSync(final SEaselStateSync packetIn, Level world) {
        EaselEntity easel = (EaselEntity) world.getEntity(packetIn.easelEntityId);

        if (easel != null) {
            easel.getStateHandler().processHistorySyncClient(packetIn.canvasCode, packetIn.snapshot, packetIn.unsyncedActions);
        } else {
            Zetter.LOG.warn("Unable to find entity " + packetIn.easelEntityId + " disregarding canvas snapshot");
        }
    }
}
