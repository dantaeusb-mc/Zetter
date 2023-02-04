package me.dantaeusb.zetter.client.gui.easel;

import com.mojang.blaze3d.matrix.MatrixStack;
import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.client.gui.EaselScreen;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;

import javax.annotation.Nullable;

abstract public class AbstractPaintingWidget extends Widget implements IGuiEventListener {
    // This is the resource location for the background image
    public static final ResourceLocation PAINTING_WIDGETS_RESOURCE = new ResourceLocation(Zetter.MOD_ID, "textures/gui/easel/widgets.png");

    protected final EaselScreen parentScreen;

    public AbstractPaintingWidget(EaselScreen parentScreen, int x, int y, int width, int height, ITextComponent title) {
        super(x, y, width, height, title);

        this.parentScreen = parentScreen;
    }

    public @Nullable ITextComponent getTooltip(int mouseX, int mouseY) {
        return this.getMessage();
    }

    public void renderLabels(MatrixStack matrixStack, int mouseX, int mouseY) {}
}
