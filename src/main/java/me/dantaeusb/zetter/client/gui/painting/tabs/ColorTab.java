package me.dantaeusb.zetter.client.gui.painting.tabs;

import com.mojang.blaze3d.vertex.PoseStack;
import me.dantaeusb.zetter.client.gui.PaintingScreen;
import me.dantaeusb.zetter.client.gui.painting.ColorCodeWidget;
import me.dantaeusb.zetter.client.gui.painting.HsbWidget;
import me.dantaeusb.zetter.core.tools.Color;
import net.minecraft.network.chat.TranslatableComponent;

public class ColorTab extends AbstractTab {
    private final ColorCodeWidget colorCodeWidget;
    private final HsbWidget slidersWidget;

    public ColorTab(PaintingScreen parentScreen, int windowX, int windowY) {
        super(parentScreen, windowX, windowY, new TranslatableComponent("container.zetter.painting.tabs.color"));

        final int SETTINGS_POSITION_X = 6;
        final int SETTINGS_POSITION_Y = 8;

        final int TEXTBOX_POSITION_X = 30;
        final int TEXTBOX_POSITION_Y = 0;

        this.slidersWidget = new HsbWidget(this.parentScreen, this.x + SETTINGS_POSITION_X, this.y + SETTINGS_POSITION_Y);
        this.colorCodeWidget = new ColorCodeWidget(this.parentScreen, this.x + TEXTBOX_POSITION_X, this.y + TEXTBOX_POSITION_Y);

        this.addTabWidget(this.slidersWidget);
        this.addTabWidget(this.colorCodeWidget);

        this.colorCodeWidget.initFields();
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTicks) {
        fill(poseStack, this.x, this.y, this.x + this.width, this.y + this.height, Color.SCREEN_GRAY.getRGB());

        this.slidersWidget.render(poseStack);
        this.colorCodeWidget.render(poseStack, this.x, this.y, partialTicks);
    }

    public void containerTick() {
        this.colorCodeWidget.tick();
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int iMouseX = (int) mouseX;
        int iMouseY = (int) mouseY;

        if (this.isMouseOver(mouseX, mouseY)) {
            this.slidersWidget.mouseClicked(iMouseX, iMouseY, button);
        }

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
        this.slidersWidget.mouseDragged(mouseX, mouseY, button, dragX, dragY);

        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.slidersWidget.mouseReleased(mouseX, mouseY, button);

        return super.mouseReleased(mouseX, mouseY, button);
    }
}
