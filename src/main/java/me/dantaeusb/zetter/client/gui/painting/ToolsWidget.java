package me.dantaeusb.zetter.client.gui.painting;

import me.dantaeusb.zetter.client.gui.PaintingScreen;
import com.mojang.blaze3d.vertex.PoseStack;
import me.dantaeusb.zetter.menu.painting.tools.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Widget;
import net.minecraft.util.StringRepresentable;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;

import javax.annotation.Nullable;
import java.util.LinkedList;
import java.util.List;

public class ToolsWidget extends AbstractPaintingWidget implements Widget {
    private final List<ToolButton> tools;

    final static int TOOLS_SIZE = 16;
    final static int TOOLS_OFFSET = TOOLS_SIZE + 1; // 1px border between slots

    public ToolsWidget(PaintingScreen parentScreen, int x, int y) {
        super(parentScreen, x, y, TOOLS_SIZE, TOOLS_SIZE + TOOLS_OFFSET * 2, new TranslatableComponent("container.zetter.painting.tools"));

        this.tools = new LinkedList<>() {{
            new ToolButton(parentScreen.getMenu().getTool(Pencil.CODE), 176, 0, TOOLS_SIZE, TOOLS_SIZE);
            new ToolButton(parentScreen.getMenu().getTool(Brush.CODE), 176, 0, TOOLS_SIZE, TOOLS_SIZE);
            new ToolButton(parentScreen.getMenu().getTool(Eyedropper.CODE), 192, 0, TOOLS_SIZE, TOOLS_SIZE);
            new ToolButton(parentScreen.getMenu().getTool(Bucket.CODE), 208, 0, TOOLS_SIZE, TOOLS_SIZE);
        }};
    }

    @Override
    public @Nullable
    Component getTooltip(int mouseX, int mouseY) {
        int i = 0;
        for (ToolButton tool: this.tools) {
            int fromY = this.y + i * TOOLS_OFFSET;

            if (PaintingScreen.isInRect(this.x, fromY, TOOLS_SIZE, TOOLS_SIZE, mouseX, mouseY)) {
                return tool.getTooltip();
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
        for (ToolButton tool: this.tools) {
            int fromY = this.y + i * TOOLS_OFFSET;

            if (PaintingScreen.isInRect(this.x, fromY, tool.width, tool.height, iMouseX, iMouseY) && this.isValidClickButton(button)) {
                this.setCurrentTool(tool);
                this.playDownSound(Minecraft.getInstance().getSoundManager());
                return true;
            }

            i++;
        }

        return false;
    }

    public void render(PoseStack matrixStack) {

        int i = 0;
        for (ToolButton tool: this.tools) {
            int fromY = this.y + i * TOOLS_OFFSET;
            int vOffset = tool.vPosition + (this.parentScreen.getMenu().getCurrentTool() == tool.getTool() ? TOOLS_SIZE : 0);

            this.blit(matrixStack, this.x, fromY, tool.uPosition, vOffset, tool.width, tool.height);
            i++;
        }
    }

    protected void setCurrentTool(ToolButton tool) {
        this.parentScreen.getMenu().setCurrentTool(tool.getTool().getCode());
    }

    public class ToolButton {
        private final AbstractTool tool;
        public final int uPosition;
        public final int vPosition;
        public final int height;
        public final int width;

        ToolButton(AbstractTool tool, int uPosition, int vPosition, int width, int height) {
            this.tool = tool;
            this.uPosition = uPosition;
            this.vPosition = vPosition;
            this.height = height;
            this.width = width;
        }

        public TranslatableComponent getTooltip() {
            return this.tool.getTranslatableComponent();
        }

        public AbstractTool getTool() {
            return this.tool;
        }
    }
}
