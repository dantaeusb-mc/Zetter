package me.dantaeusb.zetter.network;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.canvastracker.CanvasServerTracker;
import me.dantaeusb.zetter.core.Helper;
import me.dantaeusb.zetter.core.ZetterCapabilities;
import me.dantaeusb.zetter.core.ZetterItems;
import me.dantaeusb.zetter.entity.item.EaselEntity;
import me.dantaeusb.zetter.core.ZetterNetwork;
import me.dantaeusb.zetter.item.CanvasItem;
import me.dantaeusb.zetter.item.PaintingItem;
import me.dantaeusb.zetter.menu.ArtistTableMenu;
import me.dantaeusb.zetter.menu.EaselContainerMenu;
import me.dantaeusb.zetter.entity.item.state.representation.CanvasAction;
import me.dantaeusb.zetter.network.packet.*;
import me.dantaeusb.zetter.storage.AbstractCanvasData;
import me.dantaeusb.zetter.storage.CanvasData;
import me.dantaeusb.zetter.storage.DummyCanvasData;
import me.dantaeusb.zetter.storage.PaintingData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.PacketDistributor;

import java.security.InvalidParameterException;

public class ServerHandler {
    /**
     * Update canvas on server-side and send update to other tracking players
     * @param packetIn
     * @param sendingPlayer
     */
    public static void processActionBuffer(final CCanvasActionBufferPacket packetIn, ServerPlayer sendingPlayer) {
        EaselEntity easelEntity = (EaselEntity) sendingPlayer.getLevel().getEntity(packetIn.easelEntityId);

        // @todo: [MED] Check if player can access entity

        if (easelEntity != null) {
            for (CanvasAction actionBuffer : packetIn.paintingActions) {
                if (!actionBuffer.authorId.equals(sendingPlayer.getUUID())) {
                    Zetter.LOG.warn("Received action from player claimed another player UUID, ignoring");
                    return;
                }

                easelEntity.getStateHandler().processNetworkAction(actionBuffer);
            }
        } else {
            Zetter.LOG.warn("Unable to find entity " + packetIn.easelEntityId + " disregarding canvas changes");
        }
    }

    public static void processCanvasRequest(final CCanvasRequestPacket packetIn, ServerPlayer sendingPlayer) {
        // Get overworld world instance
        final MinecraftServer server = sendingPlayer.getLevel().getServer();
        final Level world = server.overworld();
        final CanvasServerTracker canvasTracker = (CanvasServerTracker) world.getCapability(ZetterCapabilities.CANVAS_TRACKER).orElse(null);
        final String canvasName = packetIn.getCanvasName();

        Zetter.LOG.debug("Got request to sync canvas " + packetIn.getCanvasName());

        if (canvasTracker == null) {
            Zetter.LOG.error("Cannot find world canvas capability");
            return;
        }

        // Notify canvas manager that player is tracking canvas from no ow
        canvasTracker.trackCanvas(sendingPlayer.getUUID(), canvasName);

        AbstractCanvasData canvasData;

        if (packetIn.getCanvasType() == AbstractCanvasData.Type.CANVAS) {
            canvasData = canvasTracker.getCanvasData(canvasName, CanvasData.class);
        } else if (packetIn.getCanvasType() == AbstractCanvasData.Type.PAINTING) {
            canvasData = canvasTracker.getCanvasData(canvasName, PaintingData.class);
        } else {
            canvasData = canvasTracker.getCanvasData(canvasName, DummyCanvasData.class);
        }

        if (canvasData == null) {
            Zetter.LOG.error("Player " + sendingPlayer + " requested non-existent canvas: " + canvasName);
            return;
        }

        if (canvasData instanceof PaintingData) {
            SPaintingSyncMessage paintingSyncMessage = new SPaintingSyncMessage(canvasName, (PaintingData) canvasData, System.currentTimeMillis());

            ZetterNetwork.simpleChannel.send(PacketDistributor.PLAYER.with(() -> sendingPlayer), paintingSyncMessage);
        } else {
            SCanvasSyncMessage canvasSyncMessage = new SCanvasSyncMessage(canvasName, canvasData, System.currentTimeMillis());

            ZetterNetwork.simpleChannel.send(PacketDistributor.PLAYER.with(() -> sendingPlayer), canvasSyncMessage);
        }
    }

    /**
     * @todo: [MED] Think about removing this
     * Not sure if it's needed, this can cause condition when canvas is unloaded while
     * other players would like to track it. Unloading on back-end should happen
     * by requests timeout and I believe this should work properly already
     *
     * @param packetIn
     * @param sendingPlayer
     */
    public static void processUnloadRequest(final CCanvasUnloadRequestPacket packetIn, ServerPlayer sendingPlayer) {
        // Get overworld world instance
        MinecraftServer server = sendingPlayer.getLevel().getServer();
        Level world = server.overworld();
        CanvasServerTracker canvasTracker = (CanvasServerTracker) world.getCapability(ZetterCapabilities.CANVAS_TRACKER).orElse(null);

        Zetter.LOG.debug("Got request to unload canvas " + packetIn.getCanvasName());

        if (canvasTracker == null) {
            Zetter.LOG.error("Cannot find world canvas capability");
            return;
        }

        canvasTracker.stopTrackingCanvas(sendingPlayer.getUUID(), packetIn.getCanvasName());
    }

    public static void processPaletteUpdate(final CPaletteUpdatePacket packetIn, ServerPlayer sendingPlayer) {
        if (sendingPlayer.containerMenu instanceof EaselContainerMenu) {
            EaselContainerMenu paintingContainer = (EaselContainerMenu)sendingPlayer.containerMenu;
            paintingContainer.setPaletteColor(packetIn.getColor(), packetIn.getSlotIndex());
        }
    }

    public static void processSignPainting(final CSignPaintingPacket packetIn, ServerPlayer sendingPlayer) {
        int slot = packetIn.getSlot();
        if (Inventory.isHotbarSlot(slot) || slot == 40) {
            ItemStack canvasStack = sendingPlayer.getInventory().getItem(slot);

            if (!canvasStack.is(ZetterItems.CANVAS.get())) {
                Zetter.LOG.error("Unable to process painting signature - item in slot is not a canvas");
                return;
            }

            CanvasData canvasData = CanvasItem.getCanvasData(canvasStack, sendingPlayer.getLevel());

            if (canvasData == null) {
                Zetter.LOG.error("Unable to process painting signature - canvas data is empty");
                return;
            }

            ItemStack paintingStack = ServerHandler.createPainting(sendingPlayer, packetIn.getPaintingTitle(), canvasData);
            sendingPlayer.getInventory().setItem(slot, paintingStack);
        }
    }

    private static ItemStack createPainting(Player player, String paintingTitle, CanvasData canvasData) {
        if (player.getLevel().isClientSide()) {
            throw new InvalidParameterException("Create painting called on client");
        }

        CanvasServerTracker canvasTracker = (CanvasServerTracker) Helper.getWorldCanvasTracker(player.getLevel());
        ItemStack outStack = new ItemStack(ZetterItems.PAINTING.get());

        /**
         * Feel like I'm getting ids before getting code always. Maybe make getCanvasCode call
         * CanvasTracker itself?
         */
        final int newId = canvasTracker.getFreePaintingId();
        final String newCode = PaintingData.getCanvasCode(newId);
        PaintingData paintingData = PaintingData.createFrom(canvasData);
        paintingData.setMetaProperties(player.getName().getString(), paintingTitle);
        canvasTracker.registerCanvasData(PaintingData.getPaintingCode(newId), paintingData);

        PaintingItem.storePaintingData(outStack, newCode, paintingData, 0);

        return outStack;
    }

    public static void processCanvasHistory(final CCanvasHistoryPacket packetIn, ServerPlayer sendingPlayer) {
        EaselEntity easelEntity = (EaselEntity) sendingPlayer.getLevel().getEntity(packetIn.easelEntityId);

        // @todo: [MED] Check if player can access entity

        if (easelEntity != null) {
            easelEntity.getStateHandler().updateActionCanceledState(packetIn.actionId, sendingPlayer.getUUID(), packetIn.canceled);
        } else {
            Zetter.LOG.warn("Unable to find entity " + packetIn.easelEntityId + " disregarding canvas changes");
        }
    }

    public static void processArtistTableModeChange(final CArtistTableModeChange packetIn, ServerPlayer sendingPlayer) {
        if (sendingPlayer.containerMenu instanceof ArtistTableMenu) {
            ArtistTableMenu artistTableMenu = (ArtistTableMenu)sendingPlayer.containerMenu;
            artistTableMenu.setMode(packetIn.getMode());
        }
    }
}
