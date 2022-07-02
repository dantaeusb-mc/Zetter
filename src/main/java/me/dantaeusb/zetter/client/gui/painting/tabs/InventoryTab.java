package me.dantaeusb.zetter.client.gui.painting.tabs;

import com.mojang.blaze3d.vertex.PoseStack;
import me.dantaeusb.zetter.client.gui.PaintingScreen;
import net.minecraft.network.chat.TranslatableComponent;

public class InventoryTab extends AbstractTab {
    public InventoryTab(PaintingScreen parentScreen, int windowX, int windowY) {
        super(parentScreen, windowX, windowY, new TranslatableComponent("container.zetter.painting.tabs.inventory"));

    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTicks) {
        // do nothing
    }
}
