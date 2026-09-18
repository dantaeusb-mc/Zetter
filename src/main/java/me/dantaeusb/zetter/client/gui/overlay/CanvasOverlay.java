package me.dantaeusb.zetter.client.gui.overlay;

import me.dantaeusb.zetter.storage.AbstractCanvasData;
import me.dantaeusb.zetter.storage.CanvasDataType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.LayeredDraw;

public interface CanvasOverlay<T extends AbstractCanvasData> extends LayeredDraw.Layer {
    String getId();

    CanvasDataType<T> getType();

    void setCanvasData(T canvasData);

    void hide();

    @Override
    void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker);

    void tick();
}