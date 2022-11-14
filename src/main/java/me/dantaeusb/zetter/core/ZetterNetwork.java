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
    // @todo: [LOW] Rename this on release, it's zetter:zetter_channel 0.1
    public static final ResourceLocation simpleChannelRL = new ResourceLocation(Zetter.MOD_ID, "zetter_channel");
    public static final String MESSAGE_PROTOCOL_VERSION = "0.2";

    public static final byte PAINTING_FRAME = 21;
    public static final byte CANVAS_REQUEST = 22;
    public static final byte PAINTING_UNLOAD_CANVAS = 23;
    public static final byte CANVAS_SYNC = 24;
    public static final byte PALETTE_UPDATE = 25;
    public static final byte PAINTING_RENAME = 26;
    public static final byte CANVAS_SYNC_VIEW = 27;
    public static final byte CANVAS_REQUEST_VIEW = 29;
    public static final byte HISTORY_SYNC = 30;
    public static final byte HISTORY_UPDATE = 31;
    public static final byte ARTIST_TABLE_MODE = 32;

    @SubscribeEvent
    @SuppressWarnings("unused")
    public static void onCommonSetupEvent(FMLCommonSetupEvent event) {
        simpleChannel = NetworkRegistry.newSimpleChannel(
                simpleChannelRL,
                () -> MESSAGE_PROTOCOL_VERSION,
                ZetterNetwork::isThisProtocolAcceptedByClient,
                ZetterNetwork::isThisProtocolAcceptedByServer
        );

        simpleChannel.registerMessage(PAINTING_FRAME, CCanvasActionPacket.class,
                CCanvasActionPacket::writePacketData, CCanvasActionPacket::readPacketData,
                CCanvasActionPacket::handle,
                Optional.of(PLAY_TO_SERVER));

        simpleChannel.registerMessage(CANVAS_REQUEST, CCanvasRequestPacket.class,
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

        simpleChannel.registerMessage(PALETTE_UPDATE, CPaletteUpdatePacket.class,
                CPaletteUpdatePacket::writePacketData, CPaletteUpdatePacket::readPacketData,
                CPaletteUpdatePacket::handle,
                Optional.of(PLAY_TO_SERVER));

        simpleChannel.registerMessage(PAINTING_RENAME, CSignPaintingPacket.class,
                CSignPaintingPacket::writePacketData, CSignPaintingPacket::readPacketData,
                CSignPaintingPacket::handle,
                Optional.of(PLAY_TO_SERVER));

        simpleChannel.registerMessage(CANVAS_SYNC_VIEW, SCanvasSyncViewMessage.class,
                SCanvasSyncViewMessage::writePacketData, SCanvasSyncViewMessage::readPacketData,
                SCanvasSyncViewMessage::handle,
                Optional.of(PLAY_TO_CLIENT));

        simpleChannel.registerMessage(CANVAS_REQUEST_VIEW, CCanvasRequestViewPacket.class,
                CCanvasRequestViewPacket::writePacketData, CCanvasRequestViewPacket::readPacketData,
                CCanvasRequestViewPacket::handle,
                Optional.of(PLAY_TO_SERVER));

        simpleChannel.registerMessage(HISTORY_SYNC, SEaselStateSync.class,
                SEaselStateSync::writePacketData, SEaselStateSync::readPacketData,
                SEaselStateSync::handle,
                Optional.of(PLAY_TO_CLIENT));

        simpleChannel.registerMessage(HISTORY_UPDATE, CCanvasHistoryActionPacket.class,
                CCanvasHistoryActionPacket::writePacketData, CCanvasHistoryActionPacket::readPacketData,
                CCanvasHistoryActionPacket::handle,
                Optional.of(PLAY_TO_SERVER));

        simpleChannel.registerMessage(ARTIST_TABLE_MODE, CArtistTableModeChange.class,
                CArtistTableModeChange::writePacketData, CArtistTableModeChange::readPacketData,
                CArtistTableModeChange::handle,
                Optional.of(PLAY_TO_SERVER));
    }

    public static boolean isThisProtocolAcceptedByClient(String protocolVersion) {
        return ZetterNetwork.MESSAGE_PROTOCOL_VERSION.equals(protocolVersion);
    }

    public static boolean isThisProtocolAcceptedByServer(String protocolVersion) {
        return ZetterNetwork.MESSAGE_PROTOCOL_VERSION.equals(protocolVersion);
    }
}
