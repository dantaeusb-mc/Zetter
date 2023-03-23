package me.dantaeusb.zetter.client.gui.easel;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import me.dantaeusb.zetter.client.gui.EaselScreen;
import me.dantaeusb.zetter.painting.Tools;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class ToolsWidget extends AbstractPaintingWidget implements Renderable {
    private final List<ToolButton> buttons;

    final static int TOOL_BUTTON_WIDTH = 22;
    final static int TOOL_BUTTON_HEIGHT = 16;

    final static int TOOL_BUTTONS_U = 209;
    final static int TOOL_BUTTONS_V = 116;

    public ToolsWidget(EaselScreen parentScreen, int x, int y) {
        // Add borders
        super(parentScreen, x, y, TOOL_BUTTON_WIDTH + 2, TOOL_BUTTON_HEIGHT * 5 + 2, Component.translatable("container.zetter.painting.tools"));

        this.buttons = new ArrayList<>() {{
            add(new ToolButton(Tools.PENCIL, TOOL_BUTTONS_U, TOOL_BUTTONS_V, TOOL_BUTTON_WIDTH, TOOL_BUTTON_HEIGHT));
            add(new ToolButton(Tools.BRUSH, TOOL_BUTTONS_U, TOOL_BUTTONS_V + TOOL_BUTTON_HEIGHT, TOOL_BUTTON_WIDTH, TOOL_BUTTON_HEIGHT));
            add(new ToolButton(Tools.EYEDROPPER, TOOL_BUTTONS_U, TOOL_BUTTONS_V + TOOL_BUTTON_HEIGHT * 2, TOOL_BUTTON_WIDTH, TOOL_BUTTON_HEIGHT));
            add(new ToolButton(Tools.BUCKET, TOOL_BUTTONS_U, TOOL_BUTTONS_V + TOOL_BUTTON_HEIGHT * 3, TOOL_BUTTON_WIDTH, TOOL_BUTTON_HEIGHT));
            add(new ToolButton(Tools.HAND, TOOL_BUTTONS_U, TOOL_BUTTONS_V + TOOL_BUTTON_HEIGHT * 4, TOOL_BUTTON_WIDTH, TOOL_BUTTON_HEIGHT));
        }};
    }

    @Override
    public @Nullable
    Component getTooltip(int mouseX, int mouseY) {
        int i = 0;
        for (ToolButton toolButton: this.buttons) {
            int fromY = this.getY() + 1 + i * TOOL_BUTTON_HEIGHT;

            if (EaselScreen.isInRect(this.getX() + 1, fromY, TOOL_BUTTON_WIDTH, TOOL_BUTTON_HEIGHT, mouseX, mouseY)) {
                return toolButton.getTooltip();
            }

            i++;
        }

        return null;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int iMouseX = (int) mouseX;
        int iMouseY = (int) mouseY;

        // Quick check
        if (!this.isMouseOver(mouseX, mouseY)) {
            return false;
        }

        int i = 0;
        for (ToolButton toolButton: this.buttons) {
            int fromY = this.getY() + 1 + i * TOOL_BUTTON_HEIGHT;

            if (EaselScreen.isInRect(this.getX(), fromY, toolButton.width, toolButton.height, iMouseX, iMouseY) && this.isValidClickButton(button)) {
                this.updateCurrentTool(toolButton);
                this.playDownSound(Minecraft.getInstance().getSoundManager());
                return true;
            }

            i++;
        }

        return false;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        narrationElementOutput.add(NarratedElementType.TITLE, this.createNarrationMessage());
    }

    public void render(PoseStack matrixStack) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, AbstractPaintingWidget.PAINTING_WIDGETS_RESOURCE);

        this.blit(matrixStack, this.getX(), this.getY(), TOOL_BUTTONS_U - TOOL_BUTTON_WIDTH - 3, TOOL_BUTTONS_V - 1, TOOL_BUTTON_WIDTH + 2, TOOL_BUTTON_HEIGHT * this.buttons.size() + 2);

        // Canvas or palette unavailable
        if (this.parentScreen.getMenu().getCanvasData() == null || this.parentScreen.getMenu().getContainer().getPaletteStack().isEmpty()) {
            return;
        }

        int i = 0;
        for (ToolButton toolButton: this.buttons) {
            int fromY = this.getY() + 1 + i * TOOL_BUTTON_HEIGHT;
            int uOffset = toolButton.uPosition + (this.parentScreen.getMenu().getCurrentTool() == toolButton.tool ? TOOL_BUTTON_WIDTH + 2 : 0);

            this.blit(matrixStack, this.getX() + 1, fromY, uOffset, toolButton.vPosition, toolButton.width, toolButton.height);
            i++;
        }
    }

    protected void updateCurrentTool(ToolButton toolButton) {
        this.parentScreen.getMenu().setCurrentTool(toolButton.tool);
    }

    public class ToolButton {
        public final Tools tool;
        public final int uPosition;
        public final int vPosition;
        public final int height;
        public final int width;

        ToolButton(Tools tool, int uPosition, int vPosition, int width, int height) {
            this.tool = tool;
            this.uPosition = uPosition;
            this.vPosition = vPosition;
            this.height = height;
            this.width = width;
        }

        public Component getTooltip() {
            return this.tool.getTool().getTranslatableComponent();
        }
    }
}
