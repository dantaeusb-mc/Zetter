package me.dantaeusb.zetter.client.gui.artisttable;

import com.mojang.blaze3d.vertex.PoseStack;
import me.dantaeusb.zetter.client.gui.ArtistTableScreen;
import me.dantaeusb.zetter.client.gui.EaselScreen;
import me.dantaeusb.zetter.core.ClientHelper;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

/**
 * @todo: [MED] Combine with another help widget?
 */
public class HelpWidget extends AbstractArtistTableWidget implements Renderable {
    final static String MANUAL_PAGE = "https://zetter.gallery/wiki/zetter#combining";

    final static int BUTTON_WIDTH = 11;
    final static int BUTTON_HEIGHT = 11;

    final static int BUTTON_POSITION_U = 0;
    final static int BUTTON_POSITION_V = 218;

    boolean clicked = false;

    public HelpWidget(ArtistTableScreen parentScreen, int x, int y) {
        super(parentScreen, x, y, BUTTON_WIDTH, BUTTON_HEIGHT, Component.translatable("container.zetter.artist_table.help"));

        if (!ClientHelper.openUriAllowed()) {
            this.active = false;
            this.visible = false;
        }
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

    @Override
    public void renderWidget(@NotNull PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        if (!this.visible) {
            return;
        }

        drawButton(matrixStack, mouseX, mouseY);
    }

    protected void drawButton(PoseStack matrixStack, int mouseX, int mouseY) {
        int buttonU = BUTTON_POSITION_U;

        if (this.clicked) {
            buttonU += BUTTON_WIDTH * 2;
        } else if (EaselScreen.isInRect(this.getX(), this.getY(), BUTTON_WIDTH, BUTTON_HEIGHT, mouseX, mouseY)) {
            buttonU += BUTTON_WIDTH;
        }

        blit(matrixStack, this.getX(), this.getY(), buttonU, BUTTON_POSITION_V, BUTTON_WIDTH, BUTTON_HEIGHT, 512, 256);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        narrationElementOutput.add(NarratedElementType.TITLE, this.createNarrationMessage());
    }
}
