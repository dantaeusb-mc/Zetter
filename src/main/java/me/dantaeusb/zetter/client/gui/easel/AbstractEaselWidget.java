package me.dantaeusb.zetter.client.gui.easel;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.client.gui.EaselScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

abstract public class AbstractEaselWidget extends AbstractWidget implements GuiEventListener {
    // This is the resource location for the background image
    public static final ResourceLocation EASEL_WIDGETS_TEXTURE_RESOURCE = new ResourceLocation(Zetter.MOD_ID, "textures/gui/easel/widgets.png");

    protected final EaselScreen parentScreen;

    public AbstractEaselWidget(EaselScreen parentScreen, int x, int y, int width, int height, Component title) {
        super(x, y, width, height, title);

        this.parentScreen = parentScreen;
    }

    public void renderTooltip(@NotNull GuiGraphics guiGraphics, int x, int y) {
        Component tooltip = this.getTooltip(x, y);

        if (tooltip != null) {
            guiGraphics.renderTooltip(this.parentScreen.getFont(), tooltip, x, y);
        }
    }

    public @Nullable Component getTooltip(int mouseX, int mouseY) {
        return this.getMessage();
    }

    public void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {}

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        narrationElementOutput.add(NarratedElementType.TITLE, this.createNarrationMessage());
    }
}
