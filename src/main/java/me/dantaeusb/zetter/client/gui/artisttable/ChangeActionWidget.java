package me.dantaeusb.zetter.client.gui.artisttable;

import com.mojang.blaze3d.vertex.PoseStack;
import me.dantaeusb.zetter.client.gui.ArtistTableScreen;
import me.dantaeusb.zetter.client.gui.EaselScreen;
import me.dantaeusb.zetter.core.ClientHelper;
import me.dantaeusb.zetter.menu.ArtistTableMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Widget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

public class ChangeActionWidget extends AbstractArtistTableWidget implements Widget {
    final static int BUTTON_WIDTH = 20;
    final static int BUTTON_HEIGHT = 18;

    final static int BUTTON_POSITION_U = 230;
    final static int BUTTON_POSITION_V = 0;

    public ChangeActionWidget(ArtistTableScreen parentScreen, int x, int y) {
        super(parentScreen, x, y, BUTTON_WIDTH, BUTTON_HEIGHT, Component.translatable("container.zetter.artist_table.change_action"));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (ArtistTableScreen.isInRect(this.x, this.y, this.width, this.height, (int) mouseX, (int) mouseY)) {
            if (!this.parentScreen.getMenu().canChangeMode()) {
                return true;
            }

            if (this.parentScreen.getMenu().getMode() == ArtistTableMenu.Mode.COMBINE) {
                this.parentScreen.getMenu().setMode(ArtistTableMenu.Mode.SPLIT);
                this.parentScreen.updateCombinedCanvasPosition();
            } else {
                this.parentScreen.getMenu().setMode(ArtistTableMenu.Mode.COMBINE);
                this.parentScreen.updateCombinedCanvasPosition();
            }

            this.playDownSound(Minecraft.getInstance().getSoundManager());
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        if (!this.visible) {
            return;
        }

        int buttonV = this.parentScreen.getMenu().getMode() == ArtistTableMenu.Mode.COMBINE ? BUTTON_POSITION_V : BUTTON_POSITION_V + BUTTON_HEIGHT * 3;

        if (!this.parentScreen.getMenu().canChangeMode()) {
            buttonV += BUTTON_HEIGHT * 2;
        } else if (ArtistTableScreen.isInRect(this.x, this.y, this.width, this.height, mouseX, mouseY)) {
            buttonV += BUTTON_HEIGHT;
        }

        blit(matrixStack, this.x, this.y, BUTTON_POSITION_U, buttonV, BUTTON_WIDTH, BUTTON_HEIGHT, 512, 256);
    }

    // @todo: add tooltip

    @Override
    public void updateNarration(NarrationElementOutput narrationElementOutput) {
        narrationElementOutput.add(NarratedElementType.TITLE, this.createNarrationMessage());
    }
}
