package me.dantaeusb.zetter.client.gui.easel.tabs;

import com.mojang.blaze3d.matrix.MatrixStack;
import me.dantaeusb.zetter.client.gui.EaselScreen;
import net.minecraft.util.text.TranslationTextComponent;

public class InventoryTab extends AbstractTab {
    public InventoryTab(EaselScreen parentScreen, int windowX, int windowY) {
        super(parentScreen, windowX, windowY, new TranslationTextComponent("container.zetter.painting.tabs.inventory"));
    }

    @Override
    public void render(MatrixStack poseStack, int mouseX, int mouseY, float partialTicks) {
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
