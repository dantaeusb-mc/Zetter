package me.dantaeusb.zetter.core;

import com.google.common.collect.Sets;
import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.ZetterConfig;
import me.dantaeusb.zetter.client.gui.PaintingScreen;
import me.dantaeusb.zetter.client.gui.overlay.CanvasOverlay;
import me.dantaeusb.zetter.event.CanvasOverlayViewEvent;
import me.dantaeusb.zetter.storage.AbstractCanvasData;
import me.dantaeusb.zetter.storage.CanvasData;
import me.dantaeusb.zetter.storage.PaintingData;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Set;

public class ClientHelper {
    private static final Set<String> ALLOWED_PROTOCOLS = Sets.newHashSet("http", "https");

    public static boolean openUriAllowed() {
        return Minecraft.getInstance().options.chatLinks().get();
    }

    public static boolean helpButtonAllowed() {
        return openUriAllowed() && ZetterConfig.CLIENT.enableHelpButton.get();
    }

    public static void openUriPrompt(Screen parentScreen, String href) {
        if (!ClientHelper.openUriAllowed()) {
            return;
        }

        try {
            URI uri = new URI(href);
            String s = uri.getScheme();
            if (s == null) {
                throw new URISyntaxException(href, "Missing protocol");
            }

            String s1 = s.toLowerCase(Locale.ROOT);
            if (!ALLOWED_PROTOCOLS.contains(s1)) {
                throw new URISyntaxException(href, "Unsupported protocol: " + s1);
            }

            Minecraft mc = Minecraft.getInstance();
            if (mc.options.chatLinksPrompt().get()) {
                mc.setScreen(new ConfirmLinkScreen((confirmed) -> {
                    if (confirmed) {
                        openUri(uri);
                    }
                    mc.setScreen(parentScreen);
                }, href, true));
            } else {
                openUri(uri);
            }
        } catch (URISyntaxException urisyntaxexception) {
            Zetter.LOG.error("Can't open url for {}", href, urisyntaxexception);
        }
    }

    public static void openCanvasScreen(Player player, String canvasCode, CanvasData canvasData, InteractionHand hand) {
        Minecraft.getInstance().setScreen(
                PaintingScreen.createScreenForCanvas(
                        player,
                        canvasCode,
                        canvasData,
                        hand
                )
        );
    }

    public static void openPaintingScreen(Player player, String canvasCode, PaintingData canvasData, InteractionHand hand) {
        Minecraft.getInstance().setScreen(
                PaintingScreen.createScreenForPainting(
                        player,
                        canvasCode,
                        canvasData,
                        hand
                )
        );
    }

    private static void openUri(URI uri) {
        Util.getPlatform().openUri(uri);
    }

    public static void showOverlay(AbstractCanvasData data) {
        for (CanvasOverlay<?> overlay : ZetterOverlays.OVERLAYS.values()) {
            overlay.hide();
        }

        CanvasOverlayViewEvent<?> viewEvent = new CanvasOverlayViewEvent<>(data);
        NeoForge.EVENT_BUS.post(viewEvent);
    }
}
