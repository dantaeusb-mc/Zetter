package me.dantaeusb.zetter.client.gui.overlay;

import com.mojang.blaze3d.vertex.PoseStack;
import me.dantaeusb.zetter.storage.AbstractCanvasData;
import me.dantaeusb.zetter.storage.CanvasDataType;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

public interface CanvasOverlay<T extends AbstractCanvasData> extends IGuiOverlay {
    String getId();

    CanvasDataType<T> getType();

    void setCanvasData(T canvasData);

    void hide();

    void render(ForgeGui gui, PoseStack poseStack, float partialTick, int screenWidth, int screenHeight);

    void tick();
}