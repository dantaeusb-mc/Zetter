package me.dantaeusb.zetter.client.gui.overlay;

import me.dantaeusb.zetter.storage.AbstractCanvasData;
import me.dantaeusb.zetter.storage.CanvasDataType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

public interface CanvasOverlay<T extends AbstractCanvasData> extends IGuiOverlay {
    String getId();

    CanvasDataType<T> getType();

    void setCanvasData(T canvasData);

    void hide();

    void render(ForgeGui gui, GuiGraphics poseStack, float partialTick, int screenWidth, int screenHeight);

    void tick();
}