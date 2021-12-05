package com.dantaeusb.zetter.core;

import com.dantaeusb.zetter.Zetter;
import com.dantaeusb.zetter.canvastracker.CanvasTrackerCapability;
import com.dantaeusb.zetter.canvastracker.ICanvasTracker;
import com.dantaeusb.zetter.client.renderer.CanvasRenderer;
import com.dantaeusb.zetter.entity.item.CustomPaintingEntity;
import com.dantaeusb.zetter.storage.AbstractCanvasData;
import com.dantaeusb.zetter.storage.CanvasData;
import com.dantaeusb.zetter.storage.DummyCanvasData;
import com.dantaeusb.zetter.storage.PaintingData;
import com.google.common.collect.Sets;
import net.minecraft.util.Util;
import net.minecraft.util.text.Style;
import net.minecraft.world.World;
import net.minecraftforge.registries.ObjectHolder;

import javax.annotation.Nullable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Set;

public class Helper {
    public static int CANVAS_COLOR = 0xFFE0DACE;

    private static Helper instance;

    public static final String COMBINED_CANVAS_CODE = Zetter.MOD_ID + "_combined_canvas";
    public static final String FALLBACK_CANVAS_CODE = Zetter.MOD_ID + "_fallback_canvas";

    private final DummyCanvasData combinedCanvas;
    private final DummyCanvasData fallbackCanvas;

    private Helper() {
        this.combinedCanvas = new DummyCanvasData(COMBINED_CANVAS_CODE);
        this.fallbackCanvas = new DummyCanvasData();
    }

    public static Helper getInstance() {
        if (Helper.instance == null) {
            Helper.instance = new Helper();
        }

        return Helper.instance;
    }

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

    public static @Nullable ICanvasTracker getWorldCanvasTracker(World world) {
        ICanvasTracker canvasTracker;

        if (!world.isClientSide()) {
            // looking for a server canvas tracker in the overworld, since canvases are world-independent
            canvasTracker = world.getServer().overworld().getCapability(CanvasTrackerCapability.CAPABILITY_CANVAS_TRACKER).orElse(null);
        } else {
            canvasTracker = world.getCapability(CanvasTrackerCapability.CAPABILITY_CANVAS_TRACKER).orElse(null);
        }

        if (canvasTracker == null) {
            Zetter.LOG.error("Cannot find world canvas capability");
            return null;
        }

        return canvasTracker;
    }

    public static DummyCanvasData getCombinedCanvas() {
        return Helper.getInstance().combinedCanvas;
    }

    public static DummyCanvasData getFallbackCanvas() {
        return Helper.getInstance().fallbackCanvas;
    }

    public static PaintingData createNewPainting(World world, AbstractCanvasData combinedCanvasData, String authorName, String title) {
        ICanvasTracker canvasTracker = Helper.getWorldCanvasTracker(world);

        int newId = canvasTracker.getNextPaintingId();
        PaintingData paintingData = new PaintingData(newId);
        paintingData.copyFrom(combinedCanvasData);
        paintingData.setMetaProperties(authorName, title);
        canvasTracker.registerCanvasData(paintingData);

        return paintingData;
    }

    public static CanvasData createNewCanvas(World worldIn) {
        ICanvasTracker canvasTracker = Helper.getWorldCanvasTracker(worldIn);

        int newId = canvasTracker.getNextCanvasId();
        CanvasData canvasData = new CanvasData(newId);
        canvasTracker.registerCanvasData(canvasData);

        return canvasData;
    }

    public static String getFrameKey(CustomPaintingEntity.Materials material, boolean plated) {
        String key = material.toString();

        if (plated) {
            key += "/plated";
        }

        return key;
    }
}
