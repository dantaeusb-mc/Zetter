package com.dantaeusb.zetter.client.gui.painting;

import com.dantaeusb.zetter.Zetter;
import com.dantaeusb.zetter.client.gui.PaintingScreen;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.IRenderable;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

public class ToolsWidget extends AbstractPaintingWidget implements IRenderable, IGuiEventListener {
    private Tool currentTool = Tool.PENCIL;

    final static int TOOLS_SIZE = 16;
    final static int TOOLS_OFFSET = TOOLS_SIZE + 1; // 1px border between slots

    public ToolsWidget(PaintingScreen parentScreen, int x, int y) {
        super(parentScreen, x, y, TOOLS_SIZE, TOOLS_SIZE + TOOLS_OFFSET * 2, new TranslationTextComponent("container.zetter.painting.tools"));
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
                this.playDownSound(Minecraft.getInstance().getSoundHandler());
                return true;
            }

            i++;
        }

        return false;
    }

    public void render(MatrixStack matrixStack) {

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

    public enum Tool implements IStringSerializable {
        PENCIL("Pencil", 176, 0),
        EYEDROPPER("Eyedropper", 192, 0),
        BUCKET("Bucket", 208, 0);

        private final String name;

        public final int uPosition;
        public final int vPosition;

        Tool(String name, int uPosition, int vPosition) {
            this.name = name;
            this.uPosition = uPosition;
            this.vPosition = vPosition;
        }

        public String toString() {
            return this.name;
        }

        public String getString() {
            return this.name;
        }
    }
}
