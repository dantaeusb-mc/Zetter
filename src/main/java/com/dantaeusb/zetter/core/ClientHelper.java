package com.dantaeusb.zetter.core;

import com.dantaeusb.zetter.Zetter;
import com.google.common.collect.Sets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.ConfirmOpenLinkScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.Util;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Set;

public class ClientHelper {
    private static final Set<String> ALLOWED_PROTOCOLS = Sets.newHashSet("http", "https");

    public static boolean openUriAllowed() {
        return Minecraft.getInstance().gameSettings.chatLinks;
    }

    /**
     * Thanks to gigaherz
     * @link {https://github.com/gigaherz/Guidebook/blob/master/src/main/java/dev/gigaherz/guidebook/guidebook/util/LinkHelper.java#L128-L193}
     * @link {Screen#handleComponentClicked(Style style)}}
     *
     * @param parentScreen
     * @param textTarget
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

            if (mc.gameSettings.chatLinksPrompt) {
                mc.displayGuiScreen(new ConfirmOpenLinkScreen((result) -> {
                    if (result) {
                        ClientHelper.openUri(uri);
                    }

                    mc.displayGuiScreen(parentScreen);
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

    private static void openUri(URI uri)
    {
        Util.getOSType().openURI(uri);
    }
}
