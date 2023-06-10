package me.dantaeusb.zetter.client.gui.easel;

import com.mojang.blaze3d.vertex.PoseStack;
import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.client.gui.EaselScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;

abstract public class AbstractPaintingWidget extends AbstractWidget implements GuiEventListener {
    // This is the resource location for the background image
    public static final ResourceLocation PAINTING_WIDGETS_RESOURCE = new ResourceLocation(Zetter.MOD_ID, "textures/gui/easel/widgets.png");

    protected final EaselScreen parentScreen;

    public AbstractPaintingWidget(EaselScreen parentScreen, int x, int y, int width, int height, Component title) {
        super(x, y, width, height, title);

        this.parentScreen = parentScreen;
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
