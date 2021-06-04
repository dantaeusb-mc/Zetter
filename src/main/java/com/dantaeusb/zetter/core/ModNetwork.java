package com.dantaeusb.zetter.core;

import com.dantaeusb.zetter.Zetter;
import com.dantaeusb.zetter.network.ClientHandler;
import com.dantaeusb.zetter.network.ServerHandler;
import com.dantaeusb.zetter.network.packet.painting.*;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;

import java.util.Optional;

import static net.minecraftforge.fml.network.NetworkDirection.PLAY_TO_CLIENT;
import static net.minecraftforge.fml.network.NetworkDirection.PLAY_TO_SERVER;

@Mod.EventBusSubscriber(modid = Zetter.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModNetwork {
    public static SimpleChannel simpleChannel;
    public static final ResourceLocation simpleChannelRL = new ResourceLocation(Zetter.MOD_ID, "zetter_channel");
    public static final String MESSAGE_PROTOCOL_VERSION = "0.1";

    public static final byte PAINTING_FRAME_CLIENT = 21;
    public static final byte PAINTING_REQUEST_CANVAS = 22;
    public static final byte PAINTING_UNLOAD_CANVAS = 23;
    public static final byte PAINTING_SYNC = 24;
    public static final byte PALETTE_UPDATE_CLIENT = 25;
    public static final byte PALETTE_UPDATE_SERVER = 26;
    public static final byte PAINTING_CREATE = 27;
    public static final byte PAINTING_SPAWN = 28;

    @SubscribeEvent
    @SuppressWarnings("unused")
    public static void onCommonSetupEvent(FMLCommonSetupEvent event) {
        simpleChannel = NetworkRegistry.newSimpleChannel(simpleChannelRL, () -> MESSAGE_PROTOCOL_VERSION,
                ClientHandler::isThisProtocolAcceptedByClient,
                ServerHandler::isThisProtocolAcceptedByServer);

        simpleChannel.registerMessage(PAINTING_FRAME_CLIENT, PaintingFrameBufferPacket.class,
                PaintingFrameBufferPacket::writePacketData, PaintingFrameBufferPacket::readPacketData,
                ServerHandler::handleFrameBuffer,
                Optional.of(PLAY_TO_SERVER));

        simpleChannel.registerMessage(PAINTING_REQUEST_CANVAS, CanvasRequestPacket.class,
                CanvasRequestPacket::writePacketData, CanvasRequestPacket::readPacketData,
                ServerHandler::handleRequestSync,
                Optional.of(PLAY_TO_SERVER));

        simpleChannel.registerMessage(PAINTING_UNLOAD_CANVAS, CanvasUnloadRequestPacket.class,
                CanvasUnloadRequestPacket::writePacketData, CanvasUnloadRequestPacket::readPacketData,
                ServerHandler::handleUnloadRequest,
                Optional.of(PLAY_TO_SERVER));

        simpleChannel.registerMessage(PAINTING_SYNC, SCanvasSyncMessage.class,
                SCanvasSyncMessage::writePacketData, SCanvasSyncMessage::readPacketData,
                ClientHandler::handleSync,
                Optional.of(PLAY_TO_CLIENT));

        simpleChannel.registerMessage(PALETTE_UPDATE_CLIENT, CPaletteUpdatePacket.class,
                CPaletteUpdatePacket::writePacketData, CPaletteUpdatePacket::readPacketData,
                ServerHandler::handlePaletteUpdate,
                Optional.of(PLAY_TO_SERVER));

        simpleChannel.registerMessage(PAINTING_CREATE, CCreatePaintingPacket.class,
                CCreatePaintingPacket::writePacketData, CCreatePaintingPacket::readPacketData,
                ServerHandler::handleCreatePainting,
                Optional.of(PLAY_TO_SERVER));
    }
}
