package com.dantaeusb.immersivemp.locks.network;

import com.dantaeusb.immersivemp.ImmersiveMp;
import com.dantaeusb.immersivemp.locks.block.LockableDoorBlock;
import com.dantaeusb.immersivemp.locks.core.ModLockNetwork;
import com.dantaeusb.immersivemp.locks.inventory.container.LockTableContainer;
import com.dantaeusb.immersivemp.locks.network.packet.CLockDoorOpen;
import com.dantaeusb.immersivemp.locks.network.packet.CLockTableModePacket;
import com.dantaeusb.immersivemp.locks.network.packet.CLockTableRenameItemPacket;
import com.dantaeusb.immersivemp.locks.tileentity.KeyLockableTileEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.state.properties.DoubleBlockHalf;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.SharedConstants;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.eventbus.api.Event;
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
     * Change Lock Table mode
     *
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

    /**
     * Try to open the door
     */
    public static void handleDoorOpen(final CLockDoorOpen packetIn, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        LogicalSide sideReceived = ctx.getDirection().getReceptionSide();
        ctx.setPacketHandled(true);

        if (sideReceived != LogicalSide.SERVER) {
            LOGGER.warn("CLockDoorOpen received on wrong side:" + ctx.getDirection().getReceptionSide());
            return;
        }

        final ServerPlayerEntity sendingPlayer = ctx.getSender();
        final World world = ctx.getSender().getServerWorld();
        if (sendingPlayer == null) {
            LOGGER.warn("EntityPlayerMP was null when CLockDoorOpen was received");
        }

        ctx.enqueueWork(() -> processDoorOpen(packetIn, world, sendingPlayer));
    }

    public static void processDoorOpen(final CLockDoorOpen packetIn, World world, ServerPlayerEntity player) {
        BlockPos pos = packetIn.getActivatedBlockPos();

        KeyLockableTileEntity doorTileEntity = LockableDoorBlock.getDoorTileEntity(world, pos);

        if (doorTileEntity == null) {
            ImmersiveMp.LOG.warn("Can't find door TE");
            return;
        }

        if (!doorTileEntity.canOpen(player, world)) {
            ImmersiveMp.LOG.info("Player cannot open this door");
            return;
        }

        doorTileEntity.openDoor();

        ImmersiveMp.LOG.info("Opening door");
    }

    public static boolean isThisProtocolAcceptedByServer(String protocolVersion) {
        return ModLockNetwork.MESSAGE_PROTOCOL_VERSION.equals(protocolVersion);
    }

    private static final Logger LOGGER = LogManager.getLogger();
}
