package me.dantaeusb.zetter.client.gui.painting;

import com.mojang.blaze3d.systems.RenderSystem;
import me.dantaeusb.zetter.client.gui.PaintingScreen;
import com.mojang.blaze3d.vertex.PoseStack;
import me.dantaeusb.zetter.painting.Tools;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Widget;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class ToolsWidget extends AbstractPaintingWidget implements Widget {
    private final List<ToolButton> buttons;

    final static int TOOL_BUTTON_WIDTH = 24;

    final static int TOOL_BUTTON_HEIGHT = 20;
    final static int TOOLS_OFFSET = TOOL_BUTTON_HEIGHT + 3; // 1px border between slots

    public ToolsWidget(PaintingScreen parentScreen, int x, int y) {
        super(parentScreen, x, y, TOOL_BUTTON_WIDTH, TOOL_BUTTON_HEIGHT + TOOLS_OFFSET * 3, new TranslatableComponent("container.zetter.painting.tools"));

        final int TOOL_BUTTON_U = 208;
        final int TOOL_BUTTON_V = 115;

        this.buttons = new ArrayList<>() {{
            add(new ToolButton(Tools.PENCIL, TOOL_BUTTON_U, TOOL_BUTTON_V, TOOL_BUTTON_WIDTH, TOOL_BUTTON_HEIGHT));
            add(new ToolButton(Tools.BRUSH, TOOL_BUTTON_U, TOOL_BUTTON_V + TOOL_BUTTON_HEIGHT, TOOL_BUTTON_WIDTH, TOOL_BUTTON_HEIGHT));
            add(new ToolButton(Tools.EYEDROPPER, TOOL_BUTTON_U, TOOL_BUTTON_V + TOOL_BUTTON_HEIGHT * 2, TOOL_BUTTON_WIDTH, TOOL_BUTTON_HEIGHT));
            add(new ToolButton(Tools.BUCKET, TOOL_BUTTON_U, TOOL_BUTTON_V + TOOL_BUTTON_HEIGHT * 3, TOOL_BUTTON_WIDTH, TOOL_BUTTON_HEIGHT));
        }};
    }

    @Override
    public @Nullable
    Component getTooltip(int mouseX, int mouseY) {
        int i = 0;
        for (ToolButton toolButton: this.buttons) {
            int fromY = this.y + i * TOOLS_OFFSET;

            if (PaintingScreen.isInRect(this.x, fromY, TOOL_BUTTON_WIDTH, TOOL_BUTTON_HEIGHT, mouseX, mouseY)) {
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
            int fromY = this.y + i * TOOLS_OFFSET;

            if (PaintingScreen.isInRect(this.x, fromY, toolButton.width, toolButton.height, iMouseX, iMouseY) && this.isValidClickButton(button)) {
                this.updateCurrentTool(toolButton);
                this.playDownSound(Minecraft.getInstance().getSoundManager());
                return true;
            }

            i++;
        }

        return false;
    }

    public void render(PoseStack matrixStack) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, AbstractPaintingWidget.PAINTING_WIDGETS_RESOURCE);

        int i = 0;
        for (ToolButton toolButton: this.buttons) {
            int fromY = this.y + i * TOOLS_OFFSET;
            int uOffset = toolButton.uPosition + (this.parentScreen.getMenu().getCurrentTool() == toolButton.tool ? TOOL_BUTTON_WIDTH : 0);

            this.blit(matrixStack, this.x, fromY, uOffset, toolButton.vPosition, toolButton.width, toolButton.height);
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

        public TranslatableComponent getTooltip() {
            return this.tool.getTool().getTranslatableComponent();
        }
    }
}
