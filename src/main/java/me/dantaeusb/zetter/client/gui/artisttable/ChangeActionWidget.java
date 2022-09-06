package me.dantaeusb.zetter.client.gui.artisttable;

import com.mojang.blaze3d.vertex.PoseStack;
import me.dantaeusb.zetter.client.gui.ArtistTableScreen;
import me.dantaeusb.zetter.client.gui.EaselScreen;
import me.dantaeusb.zetter.core.ClientHelper;
import net.minecraft.client.gui.components.Widget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.TranslatableComponent;

public class ChangeActionWidget extends AbstractArtistTableWidget implements Widget {
    final static int BUTTON_WIDTH = 20;
    final static int BUTTON_HEIGHT = 18;

    final static int BUTTON_POSITION_U = 230;
    final static int BUTTON_POSITION_V = 0;

    public ChangeActionWidget(ArtistTableScreen parentScreen, int x, int y) {
        super(parentScreen, x, y, BUTTON_WIDTH, BUTTON_HEIGHT, new TranslatableComponent("container.zetter.artist_table.help"));

        if (!ClientHelper.openUriAllowed()) {
            this.active = false;
            this.visible = false;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return super.mouseClicked(mouseX, mouseY, button);
    }

    public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        if (!this.visible) {
            return;
        }

        drawButton(matrixStack, mouseX, mouseY);
    }

    protected void drawButton(PoseStack matrixStack, int mouseX, int mouseY) {
        int buttonU = BUTTON_POSITION_U;

        this.blit(matrixStack, this.x, this.y, buttonU, BUTTON_POSITION_V, BUTTON_WIDTH, BUTTON_HEIGHT);
    }

    @Override
    public void updateNarration(NarrationElementOutput narrationElementOutput) {
        narrationElementOutput.add(NarratedElementType.TITLE, this.createNarrationMessage());
    }
}
