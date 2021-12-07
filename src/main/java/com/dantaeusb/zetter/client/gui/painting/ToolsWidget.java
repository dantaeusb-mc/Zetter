package com.dantaeusb.zetter.client.gui.painting;

import com.dantaeusb.zetter.Zetter;
import com.dantaeusb.zetter.client.gui.PaintingScreen;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Widget;
import net.minecraft.util.StringRepresentable;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;

import javax.annotation.Nullable;

public class ToolsWidget extends AbstractPaintingWidget implements Widget {
    private Tool currentTool = Tool.PENCIL;

    final static int TOOLS_SIZE = 16;
    final static int TOOLS_OFFSET = TOOLS_SIZE + 1; // 1px border between slots

    public ToolsWidget(PaintingScreen parentScreen, int x, int y) {
        super(parentScreen, x, y, TOOLS_SIZE, TOOLS_SIZE + TOOLS_OFFSET * 2, new TranslatableComponent("container.zetter.painting.tools"));
    }

    @Override
    public @Nullable
    Component getTooltip(int mouseX, int mouseY) {
        int i = 0;
        for (Tool tool: Tool.values()) {
            int fromY = this.y + i * TOOLS_OFFSET;

            if (PaintingScreen.isInRect(this.x, fromY, TOOLS_SIZE, TOOLS_SIZE, mouseX, mouseY)) {
                return tool.title;
            }

            i++;
        }

        return null;
    }

    public Tool getCurrentTool() {
        return this.currentTool;
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
        for (Tool tool: Tool.values()) {
            int fromY = this.y + i * TOOLS_OFFSET;

            if (PaintingScreen.isInRect(this.x, fromY, TOOLS_SIZE, TOOLS_SIZE, iMouseX, iMouseY) && this.isValidClickButton(button)) {
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
        for (Tool tool: Tool.values()) {
            int fromY = this.y + i * TOOLS_OFFSET;
            int vOffset = tool.vPosition + (this.currentTool == tool ? TOOLS_SIZE : 0);

            this.blit(matrixStack, this.x, fromY, tool.uPosition, vOffset, TOOLS_SIZE, TOOLS_SIZE);
            i++;
        }
    }

    protected void setCurrentTool(Tool tool) {
        this.currentTool = tool;

        this.parentScreen.updateSlidersWithCurrentColor();
    }

    public enum Tool implements StringRepresentable {
        PENCIL("Pencil", new TranslatableComponent("container.zetter.painting.tools.pencil"), 176, 0),
        EYEDROPPER("Eyedropper", new TranslatableComponent("container.zetter.painting.tools.eyedropper"), 192, 0),
        BUCKET("Bucket", new TranslatableComponent("container.zetter.painting.tools.bucket"), 208, 0);

        private final String name;
        private final TranslatableComponent title;

        public final int uPosition;
        public final int vPosition;

        Tool(String name, TranslatableComponent title, int uPosition, int vPosition) {
            this.name = name;
            this.title = title;
            this.uPosition = uPosition;
            this.vPosition = vPosition;
        }

        public String toString() {
            return this.name;
        }

        public String getSerializedName() {
            return this.name;
        }
    }
}
