package me.dantaeusb.zetter.client.gui.painting;

import com.mojang.blaze3d.systems.RenderSystem;
import me.dantaeusb.zetter.client.gui.PaintingScreen;
import me.dantaeusb.zetter.core.ClientHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.Widget;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;

import javax.annotation.Nullable;

public class HelpWidget extends AbstractPaintingWidget implements Widget {
    final static String MANUAL_PAGE = "https://zetter.gallery/wiki/zetter#painting";

    final static int BUTTON_WIDTH = 11;
    final static int BUTTON_HEIGHT = 11;

    final static int BUTTON_POSITION_U = 176;
    final static int BUTTON_POSITION_V = 174;

    boolean clicked = false;

    public HelpWidget(PaintingScreen parentScreen, int x, int y) {
        super(parentScreen, x, y, BUTTON_WIDTH, BUTTON_HEIGHT, new TranslatableComponent("container.zetter.painting.help"));

        if (!ClientHelper.openUriAllowed()) {
            this.active = false;
            this.visible = false;
        }
    }

    @Override
    public @Nullable Component getTooltip(int mouseX, int mouseY) {
        return new TranslatableComponent("container.zetter.painting.help");
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.active && this.visible) {
            int iMouseX = (int) mouseX;
            int iMouseY = (int) mouseY;

            if (PaintingScreen.isInRect(this.x, this.y, BUTTON_WIDTH, BUTTON_HEIGHT, iMouseX, iMouseY)) {
                this.clicked = true;
                ClientHelper.openUriPrompt(this.parentScreen, MANUAL_PAGE);

                return super.mouseClicked(mouseX, mouseY, button);
            }
        }

        this.clicked = false;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, PaintingScreen.PAINTING_RESOURCE);

        if (!this.visible) {
            return;
        }

        drawButton(matrixStack, mouseX, mouseY);
    }

    protected void drawButton(PoseStack matrixStack, int mouseX, int mouseY) {
        int buttonU = BUTTON_POSITION_U;

        if (this.clicked) {
            buttonU += BUTTON_WIDTH * 2;
        } else if (PaintingScreen.isInRect(this.x, this.y, BUTTON_WIDTH, BUTTON_HEIGHT, mouseX, mouseY)) {
            buttonU += BUTTON_WIDTH;
        }

        this.blit(matrixStack, this.x, this.y, buttonU, BUTTON_POSITION_V, BUTTON_WIDTH, BUTTON_HEIGHT);
    }
}
