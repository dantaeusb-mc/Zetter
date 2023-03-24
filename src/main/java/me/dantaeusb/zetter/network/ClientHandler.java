package me.dantaeusb.zetter.network;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.capability.canvastracker.CanvasTracker;
import me.dantaeusb.zetter.core.Helper;
import me.dantaeusb.zetter.core.ZetterCapabilities;
import me.dantaeusb.zetter.entity.item.EaselEntity;
import me.dantaeusb.zetter.event.CanvasViewEvent;
import me.dantaeusb.zetter.menu.ArtistTableMenu;
import me.dantaeusb.zetter.network.packet.*;
import me.dantaeusb.zetter.storage.AbstractCanvasData;
import me.dantaeusb.zetter.storage.PaintingData;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;

import java.io.IOException;

/**
 * For some reason, network executor suppresses exceptions,
 * so we catch all of those manually
 */
public class ClientHandler {
    /**
     * When canvas sent from sever, update client's canvas
     * and process update on container screens
     *
     * @param packetIn
     * @param world
     */
    public static void processCanvasSync(final SCanvasSyncPacket<?> packetIn, Level world) {
        try {
            final String canvasCode = packetIn.canvasCode;
            final AbstractCanvasData canvasData = packetIn.canvasData;
            final long timestamp = packetIn.timestamp;

            CanvasTracker canvasTracker = world.getCapability(ZetterCapabilities.CANVAS_TRACKER)
                .orElseThrow(() -> new RuntimeException("Cannot find world canvas capability"));

            canvasTracker.registerCanvasData(canvasCode, canvasData, timestamp);
        } catch (Exception e) {
            Zetter.LOG.error(e.getMessage());
            throw e;
        }
    }

    /**
     * Process SCanvasSyncViewMessage, open screen depending on
     * the type of canvas (basic or painting)
     * @param packetIn
     * @param world
     */
    public static void processCanvasSyncView(final SCanvasSyncViewPacket packetIn, Level world) {
        try {
            final LocalPlayer player = Minecraft.getInstance().player;
            final String canvasCode = packetIn.canvasCode;
            final AbstractCanvasData canvasData = packetIn.canvasData;

            CanvasViewEvent event = new CanvasViewEvent(player, canvasCode, canvasData, packetIn.getHand());

            MinecraftForge.EVENT_BUS.post(event);

            processCanvasSync(packetIn, world);
        } catch (Exception e) {
            Zetter.LOG.error(e.getMessage());
            throw e;
        }
    }

    /**
     * Process SCanvasSyncViewMessage, open screen depending on
     * the type of canvas (basic or painting)
     * @param packetIn
     * @param world
     */
    public static void processCanvasSyncExportError(final SCanvasSyncExportPacket packetIn, Level world) {
        try {
            final String canvasCode = packetIn.canvasCode;
            final PaintingData paintingData = packetIn.canvasData;

            Helper.exportPainting(Minecraft.getInstance().gameDirectory, canvasCode, paintingData);

            Minecraft.getInstance().getChatListener().handleSystemMessage(
                Component.translatable("console.zetter.result.exported_painting_client", paintingData.getPaintingName()),
                false
            );
        } catch (IOException e) {
            if (Minecraft.getInstance().getConnection() == null) {
                Zetter.LOG.error(e);
                return;
            }

            // Send message that we were unable to write file
            Minecraft.getInstance().getChatListener().handleSystemMessage(
                Component.translatable("console.zetter.error.file_write_error", e.getMessage()).withStyle(ChatFormatting.RED),
                false
            );
        } catch (Exception e) {
            Zetter.LOG.error(e.getMessage());
            throw e;
        }
    }

    /**
     * Process SCanvasSyncExportErrorPacket, show
     * player corresponding error
     *
     * @param packetIn
     * @param world
     */
    public static void processCanvasSyncExportError(final SCanvasSyncExportErrorPacket packetIn, Level world) {
        if (Minecraft.getInstance().getConnection() == null) {
            Zetter.LOG.error(packetIn.errorCode);
            return;
        }

        // Send message about result of player's request
        Minecraft.getInstance().getChatListener().handleSystemMessage(
            Component.translatable(packetIn.errorCode, packetIn.errorMessage).withStyle(ChatFormatting.RED),
            false
        );
    }

    /**
     *
     * @param packetIn
     * @param world
     */
    public static void processEaselStateSync(final SEaselStateSyncPacket packetIn, Level world) {
        try {
            EaselEntity easel = (EaselEntity) world.getEntity(packetIn.easelEntityId);

            if (easel != null) {
                easel.getStateHandler().processHistorySyncClient(packetIn.canvasCode, packetIn.sync, packetIn.snapshot, packetIn.unsyncedActions);
            } else {
                Zetter.LOG.warn("Unable to find entity " + packetIn.easelEntityId + " disregarding canvas snapshot");
            }
        } catch (Exception e) {
            Zetter.LOG.error(e.getMessage());
            throw e;
        }
    }

    /**
     * Undo and redo packets from other players
     * @param packetIn
     * @param world
     */
    public static void processCanvasHistory(final SCanvasHistoryActionPacket packetIn, Level world) {
        try {
            EaselEntity easel = (EaselEntity) world.getEntity(packetIn.easelEntityId);
            // @todo: [MED] Check if player can access entity

            if (easel != null) {
                if (packetIn.canceled) {
                    easel.getStateHandler().undo(packetIn.actionId);
                } else {
                    easel.getStateHandler().redo(packetIn.actionId);
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
     * When canvas combined on server, we need to cleanup
     * our canvas data if loaded to request the new data
     * if canvas id was reused.
     *
     * @param packetIn
     * @param world
     */
    public static void processCanvasRemoval(final SCanvasRemovalPacket packetIn, Level world) {
        try {
            final String canvasCode = packetIn.canvasCode();
            final long timestamp = packetIn.timestamp();

            CanvasTracker canvasTracker = world.getCapability(ZetterCapabilities.CANVAS_TRACKER)
                .orElseThrow(() -> new RuntimeException("Cannot find world canvas capability"));

            canvasTracker.unregisterCanvasData(canvasCode);
        } catch (Exception e) {
            Zetter.LOG.error(e.getMessage());
            throw e;
        }
    }

    /**
     * When the canvas
     * @param packetIn
     * @param world
     */
    public static void processEaselCanvasInitialization(final SEaselCanvasInitializationPacket packetIn, Level world) {
        try {
            EaselEntity easel = (EaselEntity) world.getEntity(packetIn.easelEntityId);

            // Save canvas information in texture manager
            ClientHandler.processCanvasSync(packetIn, world);

            if (easel != null) {
                easel.getStateHandler().reset();
                easel.getEaselContainer().handleCanvasChange(packetIn.canvasCode);
            } else {
                Zetter.LOG.warn("Unable to find entity " + packetIn.easelEntityId + " disregarding history reset");
            }
        } catch (Exception e) {
            Zetter.LOG.error(e.getMessage());
            throw e;
        }
    }

    /**
     * When canvas combined on server, we need to cleanup
     * our canvas data if loaded to request the new data
     * if canvas id was reused.
     *
     * @param packetIn
     * @param world
     */
    public static void processEaselReset(final SEaselResetPacket packetIn, Level world) {
        try {
            EaselEntity easel = (EaselEntity) world.getEntity(packetIn.easelEntityId);

            if (easel != null) {
                easel.getStateHandler().reset();
            } else {
                Zetter.LOG.warn("Unable to find entity " + packetIn.easelEntityId + " disregarding history reset");
            }
        } catch (Exception e) {
            Zetter.LOG.error(e.getMessage());
            throw e;
        }
    }
}
