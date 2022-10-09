package me.dantaeusb.zetter.core;

import me.dantaeusb.zetter.Zetter;
import com.google.common.collect.Sets;
import me.dantaeusb.zetter.client.gui.PaintingScreen;
import me.dantaeusb.zetter.storage.CanvasData;
import me.dantaeusb.zetter.storage.PaintingData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.Util;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Set;

public class ClientHelper {
    private static final Set<String> ALLOWED_PROTOCOLS = Sets.newHashSet("http", "https");

    public static boolean openUriAllowed() {
        return Minecraft.getInstance().options.chatLinks;
    }

    /**
     * Thanks to gigaherz
     * @link {https://github.com/gigaherz/Guidebook/blob/master/src/main/java/dev/gigaherz/guidebook/guidebook/util/LinkHelper.java#L128-L193}
     * @link {Screen#handleComponentClicked(Style style)}}
     *
     * @param parentScreen
     * @param href
     */
    public static void openUriPrompt(Screen parentScreen, String href)
    {
        Minecraft mc = Minecraft.getInstance();

        if (!ClientHelper.openUriAllowed()) {
            return;
        }

        try
        {
            URI uri = new URI(href);
            String s = uri.getScheme();
            if (s == null) {
                throw new URISyntaxException(href, "Missing protocol");
            }

            if (!ALLOWED_PROTOCOLS.contains(s.toLowerCase(Locale.ROOT))) {
                throw new URISyntaxException(href, "Unsupported protocol: " + s.toLowerCase(Locale.ROOT));
            }

            if (mc.options.chatLinksPrompt) {
                mc.setScreen(new ConfirmLinkScreen((result) -> {
                    if (result) {
                        ClientHelper.openUri(uri);
                    }

                    mc.setScreen(parentScreen);
                }, href, true));
            } else {
                ClientHelper.openUri(uri);
            }
        }
        catch (URISyntaxException e)
        {
            Zetter.LOG.error("Can't open url {}", href, e);
        }
    }

    /**
     * Show signing screen for canvases
     *
     * @param player
     * @param canvasCode
     * @param canvasData
     * @param hand
     */
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

    /**
     * Show view screen for paintings
     *
     * @param player
     * @param canvasCode
     * @param canvasData
     * @param hand
     */
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

    private static void openUri(URI uri)
    {
        Util.getPlatform().openUri(uri);
    }
}
