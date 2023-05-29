package me.dantaeusb.zetter.client.gui.easel;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import me.dantaeusb.zetter.client.gui.EaselScreen;
import me.dantaeusb.zetter.core.ClientHelper;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;

public class HelpWidget extends AbstractPaintingWidget implements Renderable {
    final static String MANUAL_PAGE = "https://zetter.gallery/wiki/zetter#painting";

    final static int BUTTON_WIDTH = 11;
    final static int BUTTON_HEIGHT = 11;

    final static int BUTTON_POSITION_U = 98;
    final static int BUTTON_POSITION_V = 0;

    boolean clicked = false;

    public HelpWidget(EaselScreen parentScreen, int x, int y) {
        super(parentScreen, x, y, BUTTON_WIDTH, BUTTON_HEIGHT, Component.translatable("container.zetter.painting.help"));

        if (!ClientHelper.openUriAllowed()) {
            this.active = false;
            this.visible = false;
        }
    }

    @Override
    public @Nullable Component getTooltip(int mouseX, int mouseY) {
        return Component.translatable("container.zetter.painting.help");
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.active && this.visible) {
            int iMouseX = (int) mouseX;
            int iMouseY = (int) mouseY;

            if (EaselScreen.isInRect(this.getX(), this.getY(), BUTTON_WIDTH, BUTTON_HEIGHT, iMouseX, iMouseY)) {
                this.clicked = true;
                ClientHelper.openUriPrompt(this.parentScreen, MANUAL_PAGE);

                return super.mouseClicked(mouseX, mouseY, button);
            }
        }

        this.clicked = false;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    public void renderWidget(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, AbstractPaintingWidget.PAINTING_WIDGETS_RESOURCE);

        drawButton(matrixStack, mouseX, mouseY);
    }

    protected void drawButton(PoseStack matrixStack, int mouseX, int mouseY) {
        int buttonU = BUTTON_POSITION_U;

        if (this.clicked) {
            buttonU += BUTTON_WIDTH * 2;
        } else if (EaselScreen.isInRect(this.getX(), this.getY(), BUTTON_WIDTH, BUTTON_HEIGHT, mouseX, mouseY)) {
            buttonU += BUTTON_WIDTH;
        }

        this.blit(matrixStack, this.getX(), this.getY(), buttonU, BUTTON_POSITION_V, BUTTON_WIDTH, BUTTON_HEIGHT);
    }
}
