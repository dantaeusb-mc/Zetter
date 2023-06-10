package me.dantaeusb.zetter.client.gui.artisttable;

import com.mojang.blaze3d.vertex.PoseStack;
import me.dantaeusb.zetter.client.gui.ArtistTableScreen;
import me.dantaeusb.zetter.client.gui.EaselScreen;
import me.dantaeusb.zetter.menu.ArtistTableMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public class ChangeActionWidget extends AbstractArtistTableWidget implements Renderable {
    private static final Component DEFAULT_TITLE = Component.translatable("container.zetter.artist_table.change_action");
    private static final Component CHANGE_TO_SPLIT_TITLE = Component.translatable("container.zetter.artist_table.change_action.to_split");
    private static final Component CHANGE_TO_COMBINE_TITLE = Component.translatable("container.zetter.artist_table.change_action.to_combine");

    private final static int BUTTON_WIDTH = 20;
    private final static int BUTTON_HEIGHT = 18;

    private final static int BUTTON_POSITION_U = 230;
    private final static int BUTTON_POSITION_V = 0;

    public ChangeActionWidget(ArtistTableScreen parentScreen, int x, int y) {
        super(parentScreen, x, y, BUTTON_WIDTH, BUTTON_HEIGHT, DEFAULT_TITLE);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (ArtistTableScreen.isInRect(this.getX(), this.getY(), this.width, this.height, (int) mouseX, (int) mouseY)) {
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

    public void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        if (!this.visible) {
            return;
        }

        int buttonV = this.parentScreen.getMenu().getMode() == ArtistTableMenu.Mode.COMBINE ? BUTTON_POSITION_V : BUTTON_POSITION_V + BUTTON_HEIGHT * 3;

        if (!this.parentScreen.getMenu().canChangeMode()) {
            buttonV += BUTTON_HEIGHT * 2;
        } else if (ArtistTableScreen.isInRect(this.getX(), this.getY(), this.width, this.height, mouseX, mouseY)) {
            buttonV += BUTTON_HEIGHT;
        }

        guiGraphics.blit(EaselScreen.EASEL_GUI_TEXTURE_RESOURCE, this.getX(), this.getY(), BUTTON_POSITION_U, buttonV, BUTTON_WIDTH, BUTTON_HEIGHT, 512, 256);
    }

    public @Nullable
    Component getTooltip(int mouseX, int mouseY) {
        if (this.parentScreen.getMenu().getMode() == ArtistTableMenu.Mode.COMBINE) {
            return CHANGE_TO_SPLIT_TITLE;
        } else {
            return CHANGE_TO_COMBINE_TITLE;
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        narrationElementOutput.add(NarratedElementType.TITLE, this.createNarrationMessage());
    }
}
