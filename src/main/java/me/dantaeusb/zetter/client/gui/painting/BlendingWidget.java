package me.dantaeusb.zetter.client.gui.painting;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import me.dantaeusb.zetter.client.gui.PaintingScreen;
import me.dantaeusb.zetter.menu.painting.pipes.BlendingPipe;
import me.dantaeusb.zetter.menu.painting.tools.Brush;
import me.dantaeusb.zetter.menu.painting.tools.Bucket;
import me.dantaeusb.zetter.menu.painting.tools.Eyedropper;
import me.dantaeusb.zetter.menu.painting.tools.Pencil;
import net.minecraft.client.gui.components.Widget;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;

import javax.annotation.Nullable;
import java.util.LinkedList;
import java.util.List;

public class BlendingWidget extends AbstractPaintingWidget implements Widget {
    private final static int WIDTH = 58;
    private final static int HEIGHT = 20;

    private final static int BLENDING_BUTTON_WIDTH = 19;

    private final static int BLENDING_BUTTON_HEIGHT = 20;

    private final List<BlendingButton> buttons;

    public BlendingWidget(PaintingScreen parentScreen, int x, int y) {
        super(parentScreen, x, y, WIDTH, HEIGHT, new TranslatableComponent("container.zetter.painting.blending"));

        final int BLENDING_BUTTON_U = 208;
        final int BLENDING_BUTTON_V = 69;

        this.buttons = new LinkedList<>() {{
            push(new BlendingButton(BlendingPipe.BlendingOption.RGB, BLENDING_BUTTON_U, BLENDING_BUTTON_V, BLENDING_BUTTON_WIDTH, BLENDING_BUTTON_HEIGHT));
            push(new BlendingButton(BlendingPipe.BlendingOption.RYB, BLENDING_BUTTON_U, BLENDING_BUTTON_V + BLENDING_BUTTON_WIDTH, BLENDING_BUTTON_WIDTH, BLENDING_BUTTON_HEIGHT));
            push(new BlendingButton(BlendingPipe.BlendingOption.RGBC, BLENDING_BUTTON_U, BLENDING_BUTTON_V + BLENDING_BUTTON_WIDTH * 2, BLENDING_BUTTON_WIDTH, BLENDING_BUTTON_HEIGHT));
        }};
    }

    @Override
    public @Nullable Component getTooltip(int mouseX, int mouseY) {
        return null;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int iMouseX = (int) mouseX;
        int iMouseY = (int) mouseY;

        if (this.isMouseOver(mouseX, mouseY)) {
            this.parentScreen.getMenu().getCurrentToolParameter("blending");
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    public void render(PoseStack matrixStack) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, AbstractPaintingWidget.PAINTING_WIDGETS_RESOURCE);


    }

    public class BlendingButton {
        private final BlendingPipe.BlendingOption blending;
        public final int uPosition;
        public final int vPosition;
        public final int height;
        public final int width;

        BlendingButton(BlendingPipe.BlendingOption blending, int uPosition, int vPosition, int width, int height) {
            this.blending = blending;
            this.uPosition = uPosition;
            this.vPosition = vPosition;
            this.height = height;
            this.width = width;
        }

        public TranslatableComponent getTooltip() {
            return this.blending.translatableComponent;
        }
    }
}
