package me.dantaeusb.zetter.client.gui.overlay;

import com.mojang.blaze3d.matrix.MatrixStack;
import me.dantaeusb.zetter.storage.AbstractCanvasData;
import me.dantaeusb.zetter.storage.CanvasDataType;
import net.minecraft.client.gui.IngameGui;

public interface CanvasOverlay<T extends AbstractCanvasData> {
    String getId();

    CanvasDataType<T> getType();

    void setCanvasData(T canvasData);

    void hide();

    void render(IngameGui gui, MatrixStack poseStack, float partialTick, int screenWidth, int screenHeight);

    void tick();
}