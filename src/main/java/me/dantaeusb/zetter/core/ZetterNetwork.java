package me.dantaeusb.zetter.core;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.network.packet.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = Zetter.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class ZetterNetwork {
    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(Zetter.MOD_ID).versioned("1.0.0");

        // Client to Server (playToServer)
        registrar.playToServer(CCanvasActionPacket.TYPE, CCanvasActionPacket.STREAM_CODEC, CCanvasActionPacket::handle);
        registrar.playToServer(CCanvasRequestPacket.TYPE, CCanvasRequestPacket.STREAM_CODEC, CCanvasRequestPacket::handle);
        registrar.playToServer(CCanvasUnloadRequestPacket.TYPE, CCanvasUnloadRequestPacket.STREAM_CODEC, CCanvasUnloadRequestPacket::handle);
        registrar.playToServer(CPaletteUpdatePacket.TYPE, CPaletteUpdatePacket.STREAM_CODEC, CPaletteUpdatePacket::handle);
        registrar.playToServer(CSignPaintingPacket.TYPE, CSignPaintingPacket.STREAM_CODEC, CSignPaintingPacket::handle);
        registrar.playToServer(CCanvasRequestViewPacket.TYPE, CCanvasRequestViewPacket.STREAM_CODEC, CCanvasRequestViewPacket::handle);
        registrar.playToServer(CCanvasHistoryActionPacket.TYPE, CCanvasHistoryActionPacket.STREAM_CODEC, CCanvasHistoryActionPacket::handle);
        registrar.playToServer(CArtistTableModeChangePacket.TYPE, CArtistTableModeChangePacket.STREAM_CODEC, CArtistTableModeChangePacket::handle);
        registrar.playToServer(CCanvasRequestExportPacket.TYPE, CCanvasRequestExportPacket.STREAM_CODEC, CCanvasRequestExportPacket::handle);

        // Server to Client (playToClient)
        registrar.playToClient(SCanvasSyncPacket.TYPE, SCanvasSyncPacket.STREAM_CODEC, SCanvasSyncPacket::handle);
        registrar.playToClient(SCanvasSyncViewPacket.TYPE, SCanvasSyncViewPacket.STREAM_CODEC, SCanvasSyncViewPacket::handle);
        registrar.playToClient(SCanvasRemovalPacket.TYPE, SCanvasRemovalPacket.STREAM_CODEC, SCanvasRemovalPacket::handle);
        registrar.playToClient(SEaselStateSyncPacket.TYPE, SEaselStateSyncPacket.STREAM_CODEC, SEaselStateSyncPacket::handle);
        registrar.playToClient(SCanvasHistoryActionPacket.TYPE, SCanvasHistoryActionPacket.STREAM_CODEC, SCanvasHistoryActionPacket::handle);
        registrar.playToClient(SEaselResetPacket.TYPE, SEaselResetPacket.STREAM_CODEC, SEaselResetPacket::handle);
        registrar.playToClient(SEaselCanvasInitializationPacket.TYPE, SEaselCanvasInitializationPacket.STREAM_CODEC, SEaselCanvasInitializationPacket::handle);
        registrar.playToClient(SCanvasSyncExportPacket.TYPE, SCanvasSyncExportPacket.STREAM_CODEC, SCanvasSyncExportPacket::handle);
        registrar.playToClient(SCanvasSyncExportErrorPacket.TYPE, SCanvasSyncExportErrorPacket.STREAM_CODEC, SCanvasSyncExportErrorPacket::handle);
    }
}
