package me.dantaeusb.zetter.network;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.capability.canvastracker.CanvasServerTracker;
import me.dantaeusb.zetter.core.Helper;
import me.dantaeusb.zetter.core.ZetterCanvasTypes;
import me.dantaeusb.zetter.core.ZetterItems;
import me.dantaeusb.zetter.core.ZetterNetwork;
import me.dantaeusb.zetter.entity.item.EaselEntity;
import me.dantaeusb.zetter.entity.item.state.representation.CanvasAction;
import me.dantaeusb.zetter.item.CanvasItem;
import me.dantaeusb.zetter.item.PaintingItem;
import me.dantaeusb.zetter.menu.ArtistTableMenu;
import me.dantaeusb.zetter.menu.EaselMenu;
import me.dantaeusb.zetter.network.packet.*;
import me.dantaeusb.zetter.storage.AbstractCanvasData;
import me.dantaeusb.zetter.storage.CanvasData;
import me.dantaeusb.zetter.storage.PaintingData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.security.InvalidParameterException;

/**
 * Handle network packets on logical server side
 *
 * N.B. For some reason, network executor suppresses exceptions,
 * so we catch all of those manually
 */
public class ServerHandler {
    /**
     * When client requests a canvas, we need to load data
     * about that canvas, and send according type of canvas in
     * sync packet
     *
     * @param canvasName
     * @param sendingPlayer
     */
    private static @Nullable AbstractCanvasData getAndTrackCanvasDataFromRequest(final String canvasName, ServerPlayer sendingPlayer) {
        final MinecraftServer server = sendingPlayer.level().getServer();
        final Level level = server.overworld();
        final CanvasServerTracker canvasTracker = (CanvasServerTracker) Helper.getLevelCanvasTracker(level);

        if (canvasTracker == null) {
            Zetter.LOG.error("Cannot find world canvas capability");
            return null;
        }

        // Notify canvas manager that player is tracking canvas from now on
        canvasTracker.trackCanvas(sendingPlayer.getUUID(), canvasName);

        AbstractCanvasData canvasData = canvasTracker.getCanvasData(canvasName);

        if (canvasData == null) {
            Zetter.LOG.error("Player " + sendingPlayer + " requested non-existent canvas: " + canvasName);
            return null;
        }

        return canvasData;
    }

    /**
     * Client asked for abstract canvas data
     * Could be just canvas, could be painting
     * @param packetIn
     * @param sendingPlayer
     */
    public static void processCanvasRequest(final CCanvasRequestPacket packetIn, ServerPlayer sendingPlayer) {
        try {
            AbstractCanvasData canvasData = getAndTrackCanvasDataFromRequest(packetIn.canvasName, sendingPlayer);
            final String canvasName = packetIn.canvasName;

            if (canvasData == null) {
                Zetter.LOG.warn("No canvas data found, not answering request for " + canvasName);
                return;
            }

            SCanvasSyncPacket canvasSyncMessage = new SCanvasSyncPacket(canvasName, canvasData, System.currentTimeMillis());

            ZetterNetwork.simpleChannel.send(PacketDistributor.PLAYER.with(() -> sendingPlayer), canvasSyncMessage);
        } catch (Exception e) {
            Zetter.LOG.error(e.getMessage());
            throw e;
        }
    }

    /**
     * When player right-clicks canvas item to preview it
     * but does not have texture data for that item.
     * Send the data in specific packet that will open
     * a GUI on requesting player's side.
     *
     * @param packetIn
     * @param sendingPlayer
     */
    public static void processCanvasViewRequest(final CCanvasRequestViewPacket packetIn, ServerPlayer sendingPlayer) {
        try {
            AbstractCanvasData canvasData = getAndTrackCanvasDataFromRequest(packetIn.canvasName, sendingPlayer);
            final String canvasName = packetIn.canvasName;

            if (canvasData == null) {
                Zetter.LOG.warn("No canvas data found, not answering view request for " + canvasName);
                return;
            }

            SCanvasSyncViewPacket canvasSyncViewMessage = new SCanvasSyncViewPacket(canvasName, canvasData, System.currentTimeMillis(), packetIn.getHand());

            ZetterNetwork.simpleChannel.send(PacketDistributor.PLAYER.with(() -> sendingPlayer), canvasSyncViewMessage);
        } catch (Exception e) {
            Zetter.LOG.error(e.getMessage());
            throw e;
        }
    }

    /**
     * When player enters export client command
     * attempt to find canvas and return in similar
     * packet so data can be saved on client
     *
     * @todo: [LOW] Check that found item is a painting
     *
     * @param packetIn
     * @param sendingPlayer
     */
    public static void processCanvasExportRequest(final CCanvasRequestExportPacket packetIn, ServerPlayer sendingPlayer) {
        try {
            final MinecraftServer server = sendingPlayer.level().getServer();
            final Level level = server.overworld();
            final CanvasServerTracker canvasTracker = (CanvasServerTracker) Helper.getLevelCanvasTracker(level);

            if (canvasTracker == null) {
                Zetter.LOG.error("Cannot find world canvas capability");

                SCanvasSyncExportErrorPacket canvasSyncExportErrorMessage = new SCanvasSyncExportErrorPacket("console.zetter.error.unknown", null);
                ZetterNetwork.simpleChannel.send(PacketDistributor.PLAYER.with(() -> sendingPlayer), canvasSyncExportErrorMessage);

                return;
            }

            String canvasCode = packetIn.requestCode;

            if (canvasCode == null) {
                canvasCode = Helper.lookupPaintingCodeByName(packetIn.requestTitle, level);
            }

            if (canvasCode == null) {
                SCanvasSyncExportErrorPacket canvasSyncExportErrorMessage = new SCanvasSyncExportErrorPacket("console.zetter.error.painting_not_found", packetIn.requestTitle);
                ZetterNetwork.simpleChannel.send(PacketDistributor.PLAYER.with(() -> sendingPlayer), canvasSyncExportErrorMessage);

                return;
            }

            PaintingData paintingData = canvasTracker.getCanvasData(canvasCode);

            if (paintingData == null) {
                SCanvasSyncExportErrorPacket canvasSyncExportErrorMessage = new SCanvasSyncExportErrorPacket("console.zetter.error.painting_not_found", canvasCode);
                ZetterNetwork.simpleChannel.send(PacketDistributor.PLAYER.with(() -> sendingPlayer), canvasSyncExportErrorMessage);

                return;
            }

            SCanvasSyncExportPacket canvasSyncExportMessage = new SCanvasSyncExportPacket(canvasCode, paintingData, System.currentTimeMillis());
            ZetterNetwork.simpleChannel.send(PacketDistributor.PLAYER.with(() -> sendingPlayer), canvasSyncExportMessage);
        } catch (Exception e) {
            Zetter.LOG.error(e.getMessage());

            SCanvasSyncExportErrorPacket canvasSyncExportErrorMessage = new SCanvasSyncExportErrorPacket("console.zetter.error.unknown", null);
            ZetterNetwork.simpleChannel.send(PacketDistributor.PLAYER.with(() -> sendingPlayer), canvasSyncExportErrorMessage);

            throw e;
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
        try {
            // Get overworld world instance
            MinecraftServer server = sendingPlayer.level().getServer();
            final Level level = server.overworld();
            final CanvasServerTracker canvasTracker = (CanvasServerTracker) Helper.getLevelCanvasTracker(level);

            Zetter.LOG.debug("Got request to unload canvas " + packetIn.getCanvasName() + " from " + sendingPlayer.getUUID());

            if (canvasTracker == null) {
                Zetter.LOG.error("Cannot find world canvas capability");
                return;
            }

            canvasTracker.stopTrackingCanvas(sendingPlayer.getUUID(), packetIn.getCanvasName());
        } catch (Exception e) {
            Zetter.LOG.error(e.getMessage());
            throw e;
        }
    }

    /**
     * When another color picked, notify server to
     * update palette item's saved colors
     *
     * @param packetIn
     * @param sendingPlayer
     */
    public static void processPaletteUpdate(final CPaletteUpdatePacket packetIn, ServerPlayer sendingPlayer) {
        try {
            if (sendingPlayer.containerMenu instanceof EaselMenu) {
                EaselMenu paintingContainer = (EaselMenu)sendingPlayer.containerMenu;
                paintingContainer.setPaletteColor(packetIn.getColor(), packetIn.getSlotIndex());
            }
        } catch (Exception e) {
            Zetter.LOG.error(e.getMessage());
            throw e;
        }
    }

    /**
     * Proces player's request to sign a painting
     * (create painting item from canvas)
     *
     * @param packetIn
     * @param sendingPlayer
     */
    public static void processSignPainting(final CSignPaintingPacket packetIn, ServerPlayer sendingPlayer) {
        try {
            int slot = packetIn.getSlot();
            if (Inventory.isHotbarSlot(slot) || slot == 40) {
                ItemStack canvasStack = sendingPlayer.getInventory().getItem(slot);

                if (!canvasStack.is(ZetterItems.CANVAS.get())) {
                    Zetter.LOG.error("Unable to process painting signature - item in slot is not a canvas");
                    return;
                }

                CanvasData canvasData = CanvasItem.getCanvasData(canvasStack, sendingPlayer.level());

                if (canvasData == null) {
                    Zetter.LOG.error("Unable to process painting signature - canvas data is empty");
                    return;
                }

                ItemStack paintingStack = ServerHandler.createPainting(sendingPlayer, packetIn.getPaintingTitle(), canvasData);
                sendingPlayer.getInventory().setItem(slot, paintingStack);
            }
        } catch (Exception e) {
            Zetter.LOG.error(e.getMessage());
            throw e;
        }
    }

    /**
     * Give player signed painting instead of canvas item
     * internal method, not a handler
     *
     * @param player
     * @param paintingTitle
     * @param canvasData
     * @return
     */
    private static ItemStack createPainting(Player player, String paintingTitle, CanvasData canvasData) {
        try {
            if (player.isLocalPlayer()) {
                throw new InvalidParameterException("Create painting called on client");
            }

            CanvasServerTracker canvasTracker = (CanvasServerTracker) Helper.getLevelCanvasTracker(player.level());
            ItemStack outStack = new ItemStack(ZetterItems.PAINTING.get());

            /**
             * Feel like I'm getting ids before getting code always. Maybe make getCanvasCode call
             * CanvasTracker itself?
             */
            final int newId = canvasTracker.getFreePaintingId();
            final String newCode = PaintingData.getCanvasCode(newId);
            PaintingData paintingData = ZetterCanvasTypes.PAINTING.get().createWrap(
                canvasData.getResolution(),
                canvasData.getWidth(),
                canvasData.getHeight(),
                canvasData.getColorData()
            );

            paintingData.setMetaProperties(player.getUUID(), player.getName().getString(), paintingTitle);
            canvasTracker.registerCanvasData(newCode, paintingData);

            PaintingItem.storePaintingData(outStack, newCode, paintingData, 0);

            return outStack;
        } catch (Exception e) {
            Zetter.LOG.error(e.getMessage());
            throw e;
        }
    }
    /**
     * Update canvas on server-side and send update to other tracking players
     * @param packetIn
     * @param sendingPlayer
     */
    public static void processAction(final CCanvasActionPacket packetIn, ServerPlayer sendingPlayer) {
        try {
            EaselEntity easelEntity = (EaselEntity) sendingPlayer.level().getEntity(packetIn.easelEntityId);

            // We don't trust client and writing our UUIDs
            for (CanvasAction actionBuffer : packetIn.paintingActions) {
                actionBuffer.setAuthorUUID(sendingPlayer.getUUID());
            }

            // @todo: [MED] Check if player can access entity

            if (easelEntity != null) {
                easelEntity.getStateHandler().processActionServer(packetIn.paintingActions);
            } else {
                Zetter.LOG.warn("Unable to find entity " + packetIn.easelEntityId + " disregarding canvas changes");
            }
        } catch (Exception e) {
            Zetter.LOG.error(e.getMessage());
            throw e;
        }
    }

    /**
     * Undo and redo packets
     * @param packetIn
     * @param sendingPlayer
     */
    public static void processCanvasHistory(final CCanvasHistoryActionPacket packetIn, ServerPlayer sendingPlayer) {
        try {
            EaselEntity easelEntity = (EaselEntity) sendingPlayer.level().getEntity(packetIn.easelEntityId);

            // @todo: [MED] Check if player can access entity

            if (easelEntity != null) {
                if (packetIn.canceled) {
                    easelEntity.getStateHandler().undo(packetIn.actionId);
                } else {
                    easelEntity.getStateHandler().redo(packetIn.actionId);
                }
            } else {
                Zetter.LOG.warn("Unable to find entity " + packetIn.easelEntityId + " disregarding canvas changes");
            }
        } catch (Exception e) {
            Zetter.LOG.error(e.getMessage());
            throw e;
        }
    }

    /**
     * Update server when we press Mode button on Artist Table
     * @param packetIn
     * @param sendingPlayer
     */
    public static void processArtistTableModeChange(final CArtistTableModeChangePacket packetIn, ServerPlayer sendingPlayer) {
        try {
            if (sendingPlayer.containerMenu instanceof ArtistTableMenu) {
                ArtistTableMenu artistTableMenu = (ArtistTableMenu)sendingPlayer.containerMenu;
                artistTableMenu.setMode(packetIn.getMode());
            }
        } catch (Exception e) {
            Zetter.LOG.error(e.getMessage());
            throw e;
        }
    }
}
