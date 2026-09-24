package me.dantaeusb.zetter.network;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.capability.canvastracker.CanvasTracker;
import me.dantaeusb.zetter.client.gui.PaintingScreen;
import me.dantaeusb.zetter.core.Helper;
import me.dantaeusb.zetter.core.ZetterCapabilities;
import me.dantaeusb.zetter.entity.item.CanvasHolderEntity;
import me.dantaeusb.zetter.event.CanvasViewEvent;
import me.dantaeusb.zetter.item.PaletteItem;
import me.dantaeusb.zetter.network.packet.*;
import me.dantaeusb.zetter.storage.AbstractCanvasData;
import me.dantaeusb.zetter.storage.PaintingData;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
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
            Zetter.LOG.error("Unable to handle processCanvasSync", e);
        }
    }

    /**
     * Process SCanvasSyncViewMessage, open screen depending on
     * the type of canvas (basic or painting)
     *
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
            Zetter.LOG.error("Unable to handle processCanvasSyncView", e);
        }
    }

    /**
     * Processes acknowledge that the canvas holder registered that this player
     * is using a certain palette (that it now tracks) for painting.
     * @param packetIn
     * @param world
     */
    public static void processCanvasHolderAcceptPacket(final SCanvasHolderAcceptPacket packetIn, Level world) {
        try {
            final LocalPlayer player = Minecraft.getInstance().player;
            assert player != null;

            // The palette this client raised a moment ago, still in the hand it used
            ItemStack paletteStack = ItemStack.EMPTY;

            for (InteractionHand hand : InteractionHand.values()) {
                if (player.getItemInHand(hand).getItem() instanceof PaletteItem) {
                    paletteStack = player.getItemInHand(hand);
                    break;
                }
            }

            if (paletteStack.isEmpty()) {
                Zetter.LOG.error("Unable to process palette use canvas holder - player is not holding a palette");
                return;
            }

            Entity canvasHolder = world.getEntity(packetIn.getCanvasHolderId());

            if (!(canvasHolder instanceof CanvasHolderEntity)) {
                Zetter.LOG.error("Unable to process palette use canvas holder - entity is not found or not a canvas holder");
                return;
            }

            ((CanvasHolderEntity) canvasHolder).addPlayerUsing(player, paletteStack);

            Minecraft.getInstance().setScreen(
                new PaintingScreen(paletteStack, (CanvasHolderEntity) canvasHolder)
            );
        } catch (Exception e) {
            Zetter.LOG.error("Unable to handle processCanvasHolderAcceptPacket", e);
        }
    }

    /**
     * Process SCanvasSyncViewMessage, open screen depending on
     * the type of canvas (basic or painting)
     *
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
            Zetter.LOG.error("Unable to handle processCanvasSyncExportError", e);
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
     * @param packetIn
     * @param world
     */
    public static void processEaselStateSync(final SEaselStateSyncPacket packetIn, Level world) {
        try {
            CanvasHolderEntity easel = (CanvasHolderEntity) world.getEntity(packetIn.easelEntityId);

            if (easel != null) {
                easel.getCanvasState().processHistorySyncClient(packetIn.canvasCode, packetIn.sync, packetIn.snapshot, packetIn.unsyncedActions);
            } else {
                Zetter.LOG.warn("Unable to find entity " + packetIn.easelEntityId + " disregarding canvas snapshot");
            }
        } catch (Exception e) {
            Zetter.LOG.error("Unable to handle processEaselStateSync", e);
        }
    }

    /**
     * Undo and redo packets from other players
     *
     * @param packetIn
     * @param world
     */
    public static void processCanvasHistory(final SCanvasHistoryActionPacket packetIn, Level world) {
        try {
            CanvasHolderEntity easel = (CanvasHolderEntity) world.getEntity(packetIn.easelEntityId);

            if (easel != null) {
                if (packetIn.canceled) {
                    easel.getCanvasState().undo(packetIn.actionId);
                } else {
                    easel.getCanvasState().redo(packetIn.actionId);
                }
            } else {
                Zetter.LOG.warn("Unable to find entity " + packetIn.easelEntityId + " disregarding canvas changes");
            }
        } catch (Exception e) {
            Zetter.LOG.error("Unable to handle processCanvasHistory", e);
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

            CanvasTracker canvasTracker = world.getCapability(ZetterCapabilities.CANVAS_TRACKER)
                .orElseThrow(() -> new RuntimeException("Cannot find world canvas capability"));

            canvasTracker.unregisterCanvasData(canvasCode);
        } catch (Exception e) {
            Zetter.LOG.error("Unable to handle processCanvasRemoval", e);
        }
    }

    /**
     * When the canvas
     *
     * @param packetIn
     * @param world
     */
    public static void processEaselCanvasInitialization(final SEaselCanvasInitializationPacket packetIn, Level world) {
        try {
            CanvasHolderEntity easel = (CanvasHolderEntity) world.getEntity(packetIn.easelEntityId);

            // Save canvas information in texture manager
            ClientHandler.processCanvasSync(packetIn, world);

            if (easel != null) {
                easel.getCanvasState().reset();
                easel.getEaselContainer().handleCanvasChange(packetIn.canvasCode);
            } else {
                Zetter.LOG.warn("Unable to find entity " + packetIn.easelEntityId + " disregarding history reset");
            }
        } catch (Exception e) {
            Zetter.LOG.error("Unable to handle processEaselCanvasInitialization", e);
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
            CanvasHolderEntity easel = (CanvasHolderEntity) world.getEntity(packetIn.easelEntityId);

            if (easel != null) {
                easel.getCanvasState().reset();
            } else {
                Zetter.LOG.warn("Unable to find entity " + packetIn.easelEntityId + " disregarding history reset");
            }
        } catch (Exception e) {
            Zetter.LOG.error("Unable to handle processEaselReset", e);
        }
    }
}
