package me.dantaeusb.zetter.core;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.network.packet.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import static net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT;
import static net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER;
import java.util.Optional;

@Mod.EventBusSubscriber(modid = Zetter.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ZetterNetwork {
    public static SimpleChannel simpleChannel;
    // @todo: rename this on release, it's zetter:zetter_channel 0.1
    public static final ResourceLocation simpleChannelRL = new ResourceLocation(Zetter.MOD_ID, "zetter_channel");
    public static final String MESSAGE_PROTOCOL_VERSION = "0.1";

    public static final byte PAINTING_FRAME = 21;
    public static final byte PAINTING_REQUEST_CANVAS = 22;
    public static final byte PAINTING_UNLOAD_CANVAS = 23;
    public static final byte CANVAS_SYNC = 24;
    public static final byte PALETTE_UPDATE = 25;
    public static final byte PAINTING_CREATE = 27;
    public static final byte EASEL_CANVAS_CHANGE = 28;
    public static final byte PAINTING_SYNC = 29;
    public static final byte PAINTING_BUCKET = 30;

    @SubscribeEvent
    @SuppressWarnings("unused")
    public static void onCommonSetupEvent(FMLCommonSetupEvent event) {
        simpleChannel = NetworkRegistry.newSimpleChannel(
                simpleChannelRL,
                () -> MESSAGE_PROTOCOL_VERSION,
                ZetterNetwork::isThisProtocolAcceptedByClient,
                ZetterNetwork::isThisProtocolAcceptedByServer
        );

        simpleChannel.registerMessage(PAINTING_FRAME, CPaintingFrameBufferPacket.class,
                CPaintingFrameBufferPacket::writePacketData, CPaintingFrameBufferPacket::readPacketData,
                CPaintingFrameBufferPacket::handle,
                Optional.of(PLAY_TO_SERVER));

        simpleChannel.registerMessage(PAINTING_REQUEST_CANVAS, CCanvasRequestPacket.class,
                CCanvasRequestPacket::writePacketData, CCanvasRequestPacket::readPacketData,
                CCanvasRequestPacket::handle,
                Optional.of(PLAY_TO_SERVER));

        simpleChannel.registerMessage(PAINTING_UNLOAD_CANVAS, CCanvasUnloadRequestPacket.class,
                CCanvasUnloadRequestPacket::writePacketData, CCanvasUnloadRequestPacket::readPacketData,
                CCanvasUnloadRequestPacket::handle,
                Optional.of(PLAY_TO_SERVER));

        simpleChannel.registerMessage(CANVAS_SYNC, SCanvasSyncMessage.class,
                SCanvasSyncMessage::writePacketData, SCanvasSyncMessage::readPacketData,
                SCanvasSyncMessage::handle,
                Optional.of(PLAY_TO_CLIENT));

        // Transfers extra data
        simpleChannel.registerMessage(PAINTING_SYNC, SPaintingSyncMessage.class,
                SPaintingSyncMessage::writePacketData, SPaintingSyncMessage::readPacketData,
                SPaintingSyncMessage::handle,
                Optional.of(PLAY_TO_CLIENT));

        simpleChannel.registerMessage(PALETTE_UPDATE, CPaletteUpdatePacket.class,
                CPaletteUpdatePacket::writePacketData, CPaletteUpdatePacket::readPacketData,
                CPaletteUpdatePacket::handle,
                Optional.of(PLAY_TO_SERVER));

        simpleChannel.registerMessage(PAINTING_CREATE, CUpdatePaintingPacket.class,
                CUpdatePaintingPacket::writePacketData, CUpdatePaintingPacket::readPacketData,
                CUpdatePaintingPacket::handle,
                Optional.of(PLAY_TO_SERVER));

        simpleChannel.registerMessage(EASEL_CANVAS_CHANGE, SEaselCanvasChangePacket.class,
                SEaselCanvasChangePacket::writePacketData, SEaselCanvasChangePacket::readPacketData,
                SEaselCanvasChangePacket::handle,
                Optional.of(PLAY_TO_CLIENT));

        simpleChannel.registerMessage(PAINTING_BUCKET, CCanvasBucketToolPacket.class,
                CCanvasBucketToolPacket::writePacketData, CCanvasBucketToolPacket::readPacketData,
                CCanvasBucketToolPacket::handle,
                Optional.of(PLAY_TO_SERVER));
    }

    public static boolean isThisProtocolAcceptedByClient(String protocolVersion) {
        return ZetterNetwork.MESSAGE_PROTOCOL_VERSION.equals(protocolVersion);
    }

    public static boolean isThisProtocolAcceptedByServer(String protocolVersion) {
        return ZetterNetwork.MESSAGE_PROTOCOL_VERSION.equals(protocolVersion);
    }
}
