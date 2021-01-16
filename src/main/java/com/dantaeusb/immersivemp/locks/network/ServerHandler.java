package com.dantaeusb.immersivemp.locks.network;

import com.dantaeusb.immersivemp.locks.core.ModLockNetwork;
import com.dantaeusb.immersivemp.locks.inventory.container.LockTableContainer;
import com.dantaeusb.immersivemp.locks.network.packet.CLockTableModePacket;
import com.dantaeusb.immersivemp.locks.network.packet.CLockTableRenameItemPacket;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.SharedConstants;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.network.NetworkEvent;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Supplier;

import static com.dantaeusb.immersivemp.locks.inventory.container.LockTableContainer.MAX_NAME_LENGTH;

public class ServerHandler {

    /**
     * Called when a message is received of the appropriate type.
     * CALLED BY THE NETWORK THREAD, NOT THE SERVER THREAD
     * @param message The message
     */
    public static void handleLockTableRenameItem(final CLockTableRenameItemPacket packetIn, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        LogicalSide sideReceived = ctx.getDirection().getReceptionSide();
        ctx.setPacketHandled(true);

        if (sideReceived != LogicalSide.SERVER) {
            LOGGER.warn("CLockTableRenameItemPacket received on wrong side:" + ctx.getDirection().getReceptionSide());
            return;
        }

        final ServerPlayerEntity sendingPlayer = ctx.getSender();
        if (sendingPlayer == null) {
            LOGGER.warn("EntityPlayerMP was null when CLockTableRenameItemPacket was received");
        }

        ctx.enqueueWork(() -> processLockTableRenameItem(packetIn, sendingPlayer));
    }

    public static void processLockTableRenameItem(final CLockTableRenameItemPacket packetIn, ServerPlayerEntity sendingPlayer) {
        if (sendingPlayer.openContainer instanceof LockTableContainer) {
            LockTableContainer lockTableContainer = (LockTableContainer)sendingPlayer.openContainer;
            String newItemName = SharedConstants.filterAllowedCharacters(packetIn.getName());

            if (newItemName.length() <= MAX_NAME_LENGTH) {
                lockTableContainer.updateItemName(newItemName);
            }
        }
    }

    /**
     * Called when a message is received of the appropriate type.
     * CALLED BY THE NETWORK THREAD, NOT THE SERVER THREAD
     * @param message The message
     */
    public static void handleLockTableMode(final CLockTableModePacket packetIn, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        LogicalSide sideReceived = ctx.getDirection().getReceptionSide();
        ctx.setPacketHandled(true);

        if (sideReceived != LogicalSide.SERVER) {
            LOGGER.warn("CLockTableModePacket received on wrong side:" + ctx.getDirection().getReceptionSide());
            return;
        }

        final ServerPlayerEntity sendingPlayer = ctx.getSender();
        if (sendingPlayer == null) {
            LOGGER.warn("EntityPlayerMP was null when CLockTableModePacket was received");
        }

        ctx.enqueueWork(() -> processLockTableMode(packetIn, sendingPlayer));
    }

    public static void processLockTableMode(final CLockTableModePacket packetIn, ServerPlayerEntity sendingPlayer) {
        if (sendingPlayer.openContainer instanceof LockTableContainer) {
            LockTableContainer lockTableContainer = (LockTableContainer)sendingPlayer.openContainer;

            lockTableContainer.updateKeyMode(packetIn.getKeyMode());
        }
    }

    public static boolean isThisProtocolAcceptedByServer(String protocolVersion) {
        return ModLockNetwork.MESSAGE_PROTOCOL_VERSION.equals(protocolVersion);
    }

    private static final Logger LOGGER = LogManager.getLogger();
}
