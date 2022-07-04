package me.dantaeusb.zetter.client.gui.painting.tabs;

import com.mojang.blaze3d.vertex.PoseStack;
import me.dantaeusb.zetter.client.gui.PaintingScreen;
import me.dantaeusb.zetter.client.gui.painting.BlendingWidget;
import me.dantaeusb.zetter.client.gui.painting.DitheringWidget;
import me.dantaeusb.zetter.client.gui.painting.SliderWidget;
import me.dantaeusb.zetter.core.tools.Color;
import me.dantaeusb.zetter.menu.painting.parameters.AbstractToolParameters;
import me.dantaeusb.zetter.menu.painting.parameters.BlendingInterface;
import me.dantaeusb.zetter.menu.painting.parameters.DitheringInterface;
import me.dantaeusb.zetter.menu.painting.parameters.IntensityInterface;
import net.minecraft.network.chat.TranslatableComponent;

public class ParametersProxyTab extends AbstractTab {
    public ParametersProxyTab(PaintingScreen parentScreen, int windowX, int windowY) {
        super(parentScreen, windowX, windowY, new TranslatableComponent("container.zetter.painting.tabs.parameters"));
    }

    @Override
    public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {

    }

    public void renderLabels(PoseStack matrixStack, int mouseX, int mouseY) {
        super.renderLabels(matrixStack, mouseX, mouseY);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return false;
    }

    /**
     * Unfortunately this event is not passed to children
     * @param mouseX
     * @param mouseY
     * @param button
     * @param dragX
     * @param dragY
     * @return
     */
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return super.mouseReleased(mouseX, mouseY, button);
    }
}
