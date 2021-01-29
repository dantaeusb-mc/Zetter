package com.dantaeusb.immersivemp.locks.core;

import com.dantaeusb.immersivemp.ImmersiveMp;
import com.dantaeusb.immersivemp.locks.network.ClientHandler;
import com.dantaeusb.immersivemp.locks.network.ServerHandler;
import com.dantaeusb.immersivemp.locks.network.handler.server.PaintingClientHandler;
import com.dantaeusb.immersivemp.locks.network.handler.server.PaintingServerHandler;
import com.dantaeusb.immersivemp.locks.network.packet.CLockDoorOpen;
import com.dantaeusb.immersivemp.locks.network.packet.CLockTableModePacket;
import com.dantaeusb.immersivemp.locks.network.packet.CLockTableRenameItemPacket;
import com.dantaeusb.immersivemp.locks.network.packet.painting.*;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;

import java.util.Optional;

import static net.minecraftforge.fml.network.NetworkDirection.PLAY_TO_CLIENT;
import static net.minecraftforge.fml.network.NetworkDirection.PLAY_TO_SERVER;

@Mod.EventBusSubscriber(modid = ImmersiveMp.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModLockNetwork {
    public static SimpleChannel simpleChannel;
    public static final ResourceLocation simpleChannelRL = new ResourceLocation(ImmersiveMp.MOD_ID, "lock_channel");
    public static final String MESSAGE_PROTOCOL_VERSION = "0.1";

    public static final byte LOCK_TABLE_ITEM_RENAME_MESSAGE_ID = 1;
    public static final byte LOCK_TABLE_MODE_MESSAGE_ID = 2;

    public static final byte LOCKING_DOOR = 11;
    public static final byte LOCKING_DOOR_RESULT = 12;

    public static final byte PAINTING_FRAME_CLIENT = 21;
    public static final byte PAINTING_REQUEST_CANVAS = 22;
    public static final byte PAINTING_UNLOAD_CANVAS = 23;
    public static final byte PAINTING_SYNC = 24;
    public static final byte PALETTE_UPDATE_CLIENT = 25;
    public static final byte PALETTE_UPDATE_SERVER = 26;

    @SubscribeEvent
    @SuppressWarnings("unused")
    public static void onCommonSetupEvent(FMLCommonSetupEvent event) {
        simpleChannel = NetworkRegistry.newSimpleChannel(simpleChannelRL, () -> MESSAGE_PROTOCOL_VERSION,
                ClientHandler::isThisProtocolAcceptedByClient,
                ServerHandler::isThisProtocolAcceptedByServer);

        simpleChannel.registerMessage(LOCK_TABLE_ITEM_RENAME_MESSAGE_ID, CLockTableRenameItemPacket.class,
                CLockTableRenameItemPacket::writePacketData, CLockTableRenameItemPacket::readPacketData,
                ServerHandler::handleLockTableRenameItem,
                Optional.of(PLAY_TO_SERVER));

        simpleChannel.registerMessage(LOCK_TABLE_MODE_MESSAGE_ID, CLockTableModePacket.class,
                CLockTableModePacket::writePacketData, CLockTableModePacket::readPacketData,
                ServerHandler::handleLockTableMode,
                Optional.of(PLAY_TO_SERVER));

        simpleChannel.registerMessage(LOCKING_DOOR, CLockDoorOpen.class,
                CLockDoorOpen::writePacketData, CLockDoorOpen::readPacketData,
                ServerHandler::handleDoorOpen,
                Optional.of(PLAY_TO_SERVER));

        // Painter

        simpleChannel.registerMessage(PAINTING_FRAME_CLIENT, PaintingFrameBufferPacket.class,
                PaintingFrameBufferPacket::writePacketData, PaintingFrameBufferPacket::readPacketData,
                PaintingServerHandler::handleFrameBuffer,
                Optional.of(PLAY_TO_SERVER));

        simpleChannel.registerMessage(PAINTING_REQUEST_CANVAS, CanvasRequestPacket.class,
                CanvasRequestPacket::writePacketData, CanvasRequestPacket::readPacketData,
                PaintingServerHandler::handleRequestSync,
                Optional.of(PLAY_TO_SERVER));

        simpleChannel.registerMessage(PAINTING_UNLOAD_CANVAS, CanvasUnloadRequestPacket.class,
                CanvasUnloadRequestPacket::writePacketData, CanvasUnloadRequestPacket::readPacketData,
                PaintingServerHandler::handleUnloadRequest,
                Optional.of(PLAY_TO_SERVER));

        simpleChannel.registerMessage(PAINTING_SYNC, SCanvasSyncMessage.class,
                SCanvasSyncMessage::writePacketData, SCanvasSyncMessage::readPacketData,
                PaintingClientHandler::handleSync,
                Optional.of(PLAY_TO_CLIENT));

        simpleChannel.registerMessage(PALETTE_UPDATE_CLIENT, CPaletteUpdatePacket.class,
                CPaletteUpdatePacket::writePacketData, CPaletteUpdatePacket::readPacketData,
                PaintingServerHandler::handlePaletteUpdate,
                Optional.of(PLAY_TO_SERVER));
    }
}
