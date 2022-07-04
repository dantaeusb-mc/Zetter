package me.dantaeusb.zetter.client.gui.painting.tabs;

import com.mojang.blaze3d.vertex.PoseStack;
import me.dantaeusb.zetter.client.gui.PaintingScreen;
import me.dantaeusb.zetter.client.gui.painting.BlendingWidget;
import me.dantaeusb.zetter.client.gui.painting.ColorCodeWidget;
import me.dantaeusb.zetter.client.gui.painting.HsbWidget;
import net.minecraft.network.chat.TranslatableComponent;

public class InventoryTab extends AbstractTab {
    public InventoryTab(PaintingScreen parentScreen, int windowX, int windowY) {
        super(parentScreen, windowX, windowY, new TranslatableComponent("container.zetter.painting.tabs.inventory"));
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTicks) {
        // do nothing
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return false;
    }
}
