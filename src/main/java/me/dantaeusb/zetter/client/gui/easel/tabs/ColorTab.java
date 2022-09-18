package me.dantaeusb.zetter.client.gui.easel.tabs;

import com.mojang.blaze3d.vertex.PoseStack;
import me.dantaeusb.zetter.client.gui.EaselScreen;
import me.dantaeusb.zetter.client.gui.easel.ColorCodeWidget;
import me.dantaeusb.zetter.client.gui.easel.HsbWidget;
import me.dantaeusb.zetter.core.tools.Color;
import net.minecraft.network.chat.TranslatableComponent;
import org.lwjgl.glfw.GLFW;

public class ColorTab extends AbstractTab {

    private final ColorCodeWidget colorCodeWidget;
    private final HsbWidget hsbWidget;

    public ColorTab(EaselScreen parentScreen, int windowX, int windowY) {
        super(parentScreen, windowX, windowY, new TranslatableComponent("container.zetter.painting.tabs.color"));

        final int SETTINGS_POSITION_X = 0;
        final int SETTINGS_POSITION_Y = 6;

        final int TEXTBOX_POSITION_X = 0;
        final int TEXTBOX_POSITION_Y = 50;

        this.hsbWidget = new HsbWidget(this.parentScreen, this.x + SETTINGS_POSITION_X, this.y + SETTINGS_POSITION_Y);
        this.colorCodeWidget = new ColorCodeWidget(this.parentScreen, this.x + TEXTBOX_POSITION_X, this.y + TEXTBOX_POSITION_Y);

        this.addTabWidget(this.hsbWidget);
        this.addTabWidget(this.colorCodeWidget);

        this.colorCodeWidget.initFields();
    }

    public void update(int color) {
        if (!this.hsbWidget.isFocused()) {
            this.hsbWidget.updateColor(color);
        }

        if (!this.colorCodeWidget.isFocused()) {
            this.colorCodeWidget.updateColorValue(color);
        }
    }

    public void update(int color, boolean triggerHsbUpdate) {
        this.hsbWidget.updateColor(color);
        this.colorCodeWidget.updateColorValue(color);
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTicks) {
        if (this.visible) {
            fill(poseStack, this.x, this.y, this.x + this.width, this.y + this.height, Color.SCREEN_GRAY.getRGB());

            this.hsbWidget.render(poseStack);
            this.colorCodeWidget.render(poseStack, this.x, this.y, partialTicks);
        }
    }

    public void containerTick() {
        this.colorCodeWidget.tick();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.active) {
            return  this.hsbWidget.mouseClicked(mouseX, mouseY, button) ||
                    this.colorCodeWidget.mouseClicked(mouseX, mouseY, button);
        }

        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return this.colorCodeWidget.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        return this.colorCodeWidget.charTyped(codePoint, modifiers);
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
        if (this.active) {
            this.hsbWidget.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }

        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.active) {
            this.hsbWidget.mouseReleased(mouseX, mouseY, button);
        }

        return false;
    }
}
