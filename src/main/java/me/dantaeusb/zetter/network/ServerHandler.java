package me.dantaeusb.zetter.network;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.capability.canvastracker.CanvasServerTracker;
import me.dantaeusb.zetter.core.Helper;
import me.dantaeusb.zetter.core.ZetterCanvasTypes;
import me.dantaeusb.zetter.core.ZetterItems;
import me.dantaeusb.zetter.core.ZetterNetwork;
import me.dantaeusb.zetter.entity.item.CanvasHolderEntity;
import me.dantaeusb.zetter.entity.item.state.representation.CanvasAction;
import me.dantaeusb.zetter.item.CanvasItem;
import me.dantaeusb.zetter.item.PaintingItem;
import me.dantaeusb.zetter.item.ChalkItem;
import me.dantaeusb.zetter.item.PaletteItem;
import me.dantaeusb.zetter.menu.ArtistTableMenu;
import me.dantaeusb.zetter.network.packet.*;
import me.dantaeusb.zetter.storage.AbstractCanvasData;
import me.dantaeusb.zetter.storage.CanvasData;
import me.dantaeusb.zetter.storage.PaintingData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
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
            Zetter.LOG.error("Unable to handle processCanvasRequest", e);
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
            Zetter.LOG.error("Unable to handle processCanvasViewRequest", e);
        }
    }

    /**
     * Resolve a canvas holder the player is allowed to act on. Everything the client
     * sends here is an entity id off the wire, so it has to be checked every time.
     *
     * @param sendingPlayer
     * @param canvasHolderId
     * @param requireUsing whether the player must have started using the holder already
     * @return
     */
    private static @Nullable CanvasHolderEntity getAccessibleCanvasHolder(ServerPlayer sendingPlayer, int canvasHolderId, boolean requireUsing) {
        final Entity entity = sendingPlayer.level().getEntity(canvasHolderId);

        if (!(entity instanceof CanvasHolderEntity canvasHolder)) {
            Zetter.LOG.warn("Player " + sendingPlayer.getName().getString() + " referenced entity " + canvasHolderId + " which is not a canvas holder");
            return null;
        }

        if (!canvasHolder.canPlayerAccessInventory(sendingPlayer)) {
            Zetter.LOG.warn("Player " + sendingPlayer.getName().getString() + " cannot reach canvas holder " + canvasHolderId);
            return null;
        }

        if (requireUsing && !canvasHolder.getPlayersUsing().contains(sendingPlayer)) {
            Zetter.LOG.warn("Player " + sendingPlayer.getName().getString() + " is not using canvas holder " + canvasHolderId);
            return null;
        }

        return canvasHolder;
    }

    /**
     * When player uses palette item on canvas holder
     * we need to update canvas holder's palette and
     * add player to current users
     *
     * @param packetIn
     * @param sendingPlayer
     */
    public static void processPaletteUseCanvasHolder(final CPaletteUseCanvasHolderPacket packetIn, ServerPlayer sendingPlayer) {
        try {
            final ItemStack paletteStack = sendingPlayer.getItemInHand(packetIn.getHand());

            if (!(paletteStack.getItem() instanceof PaletteItem)) {
                Zetter.LOG.warn("Player " + sendingPlayer.getName().getString() + " is not holding a palette");
                return;
            }

            final CanvasHolderEntity canvasHolder = getAccessibleCanvasHolder(sendingPlayer, packetIn.getCanvasHolderId(), false);

            if (canvasHolder == null) {
                return;
            }

            if (!canvasHolder.acceptsImplement(paletteStack)) {
                Zetter.LOG.warn("Player " + sendingPlayer.getName().getString() + " cannot use a palette on canvas holder " + canvasHolder.getId());
                return;
            }

            // Same rule PaletteItem applies on the client when it picks a holder to aim at
            if (!canvasHolder.canPlayerStartUsing(sendingPlayer)) {
                Zetter.LOG.warn("Player " + sendingPlayer.getName().getString() + " cannot start using canvas holder " + canvasHolder.getId());
                return;
            }

            canvasHolder.addPlayerUsing(sendingPlayer, paletteStack);
            ZetterNetwork.simpleChannel.send(PacketDistributor.PLAYER.with(() -> sendingPlayer), new SCanvasHolderAcceptPacket(canvasHolder.getId()));
        } catch (Exception e) {
            Zetter.LOG.error("Unable to handle processPaletteUseCanvasHolder", e);
        }
    }

    /**
     * Player started drawing on a board with chalk. No screen opens and no accept
     * comes back: the client has already started drawing locally, and if this is
     * refused its actions are simply dropped when they arrive.
     *
     * @param packetIn
     * @param sendingPlayer
     */
    public static void processChalkUseCanvasHolder(final CChalkUseCanvasHolderPacket packetIn, ServerPlayer sendingPlayer) {
        try {
            final ItemStack chalkStack = sendingPlayer.getItemInHand(packetIn.getHand());

            if (!(chalkStack.getItem() instanceof ChalkItem)) {
                Zetter.LOG.warn("Player " + sendingPlayer.getName().getString() + " is not holding chalk");
                return;
            }

            final CanvasHolderEntity canvasHolder = getAccessibleCanvasHolder(sendingPlayer, packetIn.getCanvasHolderId(), false);

            if (canvasHolder == null) {
                return;
            }

            if (!canvasHolder.acceptsImplement(chalkStack)) {
                Zetter.LOG.warn("Player " + sendingPlayer.getName().getString() + " cannot use chalk on canvas holder " + canvasHolder.getId());
                return;
            }

            if (!canvasHolder.canPlayerStartUsing(sendingPlayer)) {
                Zetter.LOG.warn("Player " + sendingPlayer.getName().getString() + " cannot start using canvas holder " + canvasHolder.getId());
                return;
            }

            canvasHolder.addPlayerUsing(sendingPlayer, chalkStack);
        } catch (Exception e) {
            Zetter.LOG.error("Unable to handle processChalkUseCanvasHolder", e);
        }
    }

    /**
     * Player closed the painting screen, drop them and their palette from the holder
     *
     * @param packetIn
     * @param sendingPlayer
     */
    public static void processCanvasHolderStopUsing(final CCanvasHolderStopUsingPacket packetIn, ServerPlayer sendingPlayer) {
        try {
            if (!(sendingPlayer.level().getEntity(packetIn.getCanvasHolderId()) instanceof CanvasHolderEntity canvasHolder)) {
                Zetter.LOG.warn("Unable to process stop using canvas holder - entity is not found or not a canvas holder");
                return;
            }

            // No reach check: dropping yourself is harmless wherever you ended up
            canvasHolder.removePlayerUsing(sendingPlayer);
        } catch (Exception e) {
            Zetter.LOG.error("Unable to handle processCanvasHolderStopUsing", e);
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
            Zetter.LOG.error("Unable to handle processCanvasExportRequest", e);

            SCanvasSyncExportErrorPacket canvasSyncExportErrorMessage = new SCanvasSyncExportErrorPacket("console.zetter.error.unknown", null);
            ZetterNetwork.simpleChannel.send(PacketDistributor.PLAYER.with(() -> sendingPlayer), canvasSyncExportErrorMessage);
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
            Zetter.LOG.error("Unable to handle processUnloadRequest", e);
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
            ItemStack paletteStack = Helper.lookupPaletteStackByPlayer(sendingPlayer, packetIn.getPaletteUuid());

            if (paletteStack.isEmpty()) {
                Zetter.LOG.error("Unable to process palette update - item in slot is not a palette");
                return;
            }

            PaletteItem.updatePaletteColor(paletteStack, packetIn.getColor(), packetIn.getSlotIndex());
        } catch (Exception e) {
            Zetter.LOG.error("Unable to handle processPaletteUpdate", e);
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
            Zetter.LOG.error("Unable to handle processSignPainting", e);
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
            // Caller needs the stack, so this one still has to propagate
            Zetter.LOG.error("Unable to create painting", e);
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
            final CanvasHolderEntity easelEntity = getAccessibleCanvasHolder(sendingPlayer, packetIn.easelEntityId, true);

            if (easelEntity == null) {
                return;
            }

            // We don't trust client and writing our UUIDs
            for (CanvasAction actionBuffer : packetIn.paintingActions) {
                actionBuffer.setAuthorUUID(sendingPlayer.getUUID());
            }

            easelEntity.getCanvasState().processActionServer(packetIn.paintingActions);
        } catch (Exception e) {
            Zetter.LOG.error("Unable to handle processAction", e);
        }
    }

    /**
     * Undo and redo packets
     * @param packetIn
     * @param sendingPlayer
     */
    public static void processCanvasHistory(final CCanvasHistoryActionPacket packetIn, ServerPlayer sendingPlayer) {
        try {
            final CanvasHolderEntity easelEntity = getAccessibleCanvasHolder(sendingPlayer, packetIn.easelEntityId, true);

            if (easelEntity == null) {
                return;
            }

            if (packetIn.canceled) {
                easelEntity.getCanvasState().undo(packetIn.actionId);
            } else {
                easelEntity.getCanvasState().redo(packetIn.actionId);
            }
        } catch (Exception e) {
            Zetter.LOG.error("Unable to handle processCanvasHistory", e);
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
            Zetter.LOG.error("Unable to handle processArtistTableModeChange", e);
        }
    }
}
