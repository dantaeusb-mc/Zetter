package me.dantaeusb.zetter.core;

import com.google.common.collect.Sets;
import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.ZetterConfig;
import me.dantaeusb.zetter.client.gui.CanvasScreen;
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
import net.minecraftforge.common.MinecraftForge;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Set;

/**
 * Helper to handle events and combination of the combined canvas when stitching.
 */
public class ClientCombinedCanvasHelper {
}
