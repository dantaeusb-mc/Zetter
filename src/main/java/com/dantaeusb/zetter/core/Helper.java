package com.dantaeusb.zetter.core;

import com.dantaeusb.zetter.Zetter;
import com.dantaeusb.zetter.canvastracker.CanvasTrackerCapability;
import com.dantaeusb.zetter.canvastracker.ICanvasTracker;
import com.dantaeusb.zetter.client.renderer.CanvasRenderer;
import com.dantaeusb.zetter.storage.CanvasData;
import com.dantaeusb.zetter.storage.DummyCanvasData;
import com.dantaeusb.zetter.storage.PaintingData;
import net.minecraft.client.Minecraft;
import net.minecraft.world.World;
import net.minecraftforge.registries.ObjectHolder;

import javax.annotation.Nullable;

public class Helper {
    private static Helper instance;

    public static final int CANVAS_TEXTURE_RESOLUTION = 16;

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

    public static @Nullable ICanvasTracker getWorldCanvasTracker(World world) {
        ICanvasTracker canvasTracker;

        if (!world.isRemote()) {
            // looking for a server canvas tracker in the overworld, since canvases are world-independent
            canvasTracker = world.getServer().func_241755_D_().getCapability(CanvasTrackerCapability.CAPABILITY_CANVAS_TRACKER).orElse(null);
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

    public static PaintingData createNewPainting(World worldIn) {
        ICanvasTracker canvasTracker = Helper.getWorldCanvasTracker(worldIn);

        int newId = canvasTracker.getNextPaintingId();
        PaintingData paintingData = new PaintingData(newId);
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

    private class DataStorage {

    }
}
