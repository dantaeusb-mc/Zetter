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
    public static final String MESSAGE_PROTOCOL_VERSION = "0.3";

    public static final byte PAINTING_FRAME = 21;
    public static final byte CANVAS_REQUEST = 22;
    public static final byte CANVAS_SYNC = 24;

    public static final byte PAINTING_UNLOAD_CANVAS = 23;

    public static final byte PALETTE_UPDATE = 25;
    public static final byte PAINTING_RENAME = 26;

    public static final byte CANVAS_REQUEST_SYNC_VIEW = 28;
    public static final byte CANVAS_SYNC_VIEW = 27;

    public static final byte CANVAS_REMOVE = 29;
    public static final byte EASEL_SYNC = 30;

    public static final byte HISTORY_UPDATE = 31;
    public static final byte HISTORY_SYNC = 33;
    public static final byte HISTORY_RESET = 34;

    public static final byte ARTIST_TABLE_MODE = 32;

    public static final byte EASEL_CANVAS_INIT = 35;

    public static final byte CANVAS_REQUEST_EXPORT = 40;
    public static final byte CANVAS_EXPORT = 41;
    public static final byte CANVAS_EXPORT_ERROR = 42;

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

        simpleChannel.registerMessage(CANVAS_SYNC, SCanvasSyncPacket.class,
            SCanvasSyncPacket::writePacketData, SCanvasSyncPacket::readPacketData,
            SCanvasSyncPacket::handle,
            Optional.of(PLAY_TO_CLIENT));

        simpleChannel.registerMessage(PALETTE_UPDATE, CPaletteUpdatePacket.class,
            CPaletteUpdatePacket::writePacketData, CPaletteUpdatePacket::readPacketData,
            CPaletteUpdatePacket::handle,
            Optional.of(PLAY_TO_SERVER));

        simpleChannel.registerMessage(PAINTING_RENAME, CSignPaintingPacket.class,
            CSignPaintingPacket::writePacketData, CSignPaintingPacket::readPacketData,
            CSignPaintingPacket::handle,
            Optional.of(PLAY_TO_SERVER));

        simpleChannel.registerMessage(CANVAS_SYNC_VIEW, SCanvasSyncViewPacket.class,
            SCanvasSyncViewPacket::writePacketData, SCanvasSyncViewPacket::readPacketData,
            SCanvasSyncViewPacket::handle,
            Optional.of(PLAY_TO_CLIENT));

        simpleChannel.registerMessage(CANVAS_REQUEST_SYNC_VIEW, CCanvasRequestViewPacket.class,
            CCanvasRequestViewPacket::writePacketData, CCanvasRequestViewPacket::readPacketData,
            CCanvasRequestViewPacket::handle,
            Optional.of(PLAY_TO_SERVER));

        simpleChannel.registerMessage(CANVAS_REMOVE, SCanvasRemovalPacket.class,
            SCanvasRemovalPacket::writePacketData, SCanvasRemovalPacket::readPacketData,
            SCanvasRemovalPacket::handle,
            Optional.of(PLAY_TO_CLIENT));

        simpleChannel.registerMessage(EASEL_SYNC, SEaselStateSyncPacket.class,
            SEaselStateSyncPacket::writePacketData, SEaselStateSyncPacket::readPacketData,
            SEaselStateSyncPacket::handle,
            Optional.of(PLAY_TO_CLIENT));

        simpleChannel.registerMessage(HISTORY_UPDATE, CCanvasHistoryActionPacket.class,
            CCanvasHistoryActionPacket::writePacketData, CCanvasHistoryActionPacket::readPacketData,
            CCanvasHistoryActionPacket::handle,
            Optional.of(PLAY_TO_SERVER));

        simpleChannel.registerMessage(ARTIST_TABLE_MODE, CArtistTableModeChangePacket.class,
            CArtistTableModeChangePacket::writePacketData, CArtistTableModeChangePacket::readPacketData,
            CArtistTableModeChangePacket::handle,
            Optional.of(PLAY_TO_SERVER));

        simpleChannel.registerMessage(HISTORY_SYNC, SCanvasHistoryActionPacket.class,
            SCanvasHistoryActionPacket::writePacketData, SCanvasHistoryActionPacket::readPacketData,
            SCanvasHistoryActionPacket::handle,
            Optional.of(PLAY_TO_CLIENT));

        simpleChannel.registerMessage(HISTORY_RESET, SEaselResetPacket.class,
            SEaselResetPacket::writePacketData, SEaselResetPacket::readPacketData,
            SEaselResetPacket::handle,
            Optional.of(PLAY_TO_CLIENT));

        simpleChannel.registerMessage(EASEL_CANVAS_INIT, SEaselCanvasInitializationPacket.class,
            SEaselCanvasInitializationPacket::writePacketData, SEaselCanvasInitializationPacket::readPacketData,
            SEaselCanvasInitializationPacket::handle,
            Optional.of(PLAY_TO_CLIENT));

        simpleChannel.registerMessage(CANVAS_REQUEST_EXPORT, CCanvasRequestExportPacket.class,
            CCanvasRequestExportPacket::writePacketData, CCanvasRequestExportPacket::readPacketData,
            CCanvasRequestExportPacket::handle,
            Optional.of(PLAY_TO_SERVER));

        simpleChannel.registerMessage(CANVAS_EXPORT, SCanvasSyncExportPacket.class,
            SCanvasSyncExportPacket::writePacketData, SCanvasSyncExportPacket::readPacketData,
            SCanvasSyncExportPacket::handle,
            Optional.of(PLAY_TO_CLIENT));

        simpleChannel.registerMessage(CANVAS_EXPORT_ERROR, SCanvasSyncExportErrorPacket.class,
            SCanvasSyncExportErrorPacket::writePacketData, SCanvasSyncExportErrorPacket::readPacketData,
            SCanvasSyncExportErrorPacket::handle,
            Optional.of(PLAY_TO_CLIENT));
    }

    public static boolean isThisProtocolAcceptedByClient(String protocolVersion) {
        return ZetterNetwork.MESSAGE_PROTOCOL_VERSION.equals(protocolVersion);
    }

    public static boolean isThisProtocolAcceptedByServer(String protocolVersion) {
        return ZetterNetwork.MESSAGE_PROTOCOL_VERSION.equals(protocolVersion);
    }
}
