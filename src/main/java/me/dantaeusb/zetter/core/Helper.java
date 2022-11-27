package me.dantaeusb.zetter.core;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.canvastracker.ICanvasTracker;
import me.dantaeusb.zetter.entity.item.CustomPaintingEntity;
import me.dantaeusb.zetter.storage.AbstractCanvasData;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/**
 * @todo: [MID] Get rid of this class, all functions can be moved to the classes with execution context
 */
public class Helper {
    public static final int DUMMY_BLACK_COLOR = 0xFF000000;
    public static final int DUMMY_PINK_COLOR = 0xFFFF00FF;
    public static final int CANVAS_COLOR = 0xFFE0DACE;

    public static final String COMBINED_CANVAS_CODE = Zetter.MOD_ID + "_combined_canvas";
    public static final String FALLBACK_CANVAS_CODE = Zetter.MOD_ID + "_fallback_canvas";

    public static final int CANVAS_CODE_MAX_LENGTH = 64;

    /**
     * Resolution for GUIs
     * @return
     */
    public static AbstractCanvasData.Resolution getBasicResolution() {
        return AbstractCanvasData.Resolution.x16;
    }

    public static AbstractCanvasData.Resolution getResolution() {
        return AbstractCanvasData.Resolution.x16;
    }

    public static ICanvasTracker getWorldCanvasTracker(Level world) {
        ICanvasTracker canvasTracker;

        if (!world.isClientSide()) {
            // looking for a server canvas tracker in the overworld, since canvases are world-independent
            canvasTracker = world.getServer().overworld().getCapability(ZetterCapabilities.CANVAS_TRACKER).orElse(null);
        } else {
            canvasTracker = world.getCapability(ZetterCapabilities.CANVAS_TRACKER).orElse(null);
        }

        return canvasTracker;
    }

    public static String getFrameKey(CustomPaintingEntity.Materials material, boolean plated) {
        String key = material.toString();

        if (plated) {
            key += "/plated";
        }

        return key;
    }
}
