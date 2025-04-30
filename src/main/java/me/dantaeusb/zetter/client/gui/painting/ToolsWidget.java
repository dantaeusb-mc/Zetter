package me.dantaeusb.zetter.client.gui.painting;

import me.dantaeusb.zetter.client.gui.PaintingScreen;
import me.dantaeusb.zetter.painting.Tool;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class ToolsWidget extends AbstractPaintingWidget implements Renderable {
    private final List<ToolButton> buttons;

    final static int TOOL_BUTTON_WIDTH = 22;
    final static int TOOL_BUTTON_HEIGHT = 16;

    final static int TOOL_BUTTONS_U = 0;
    final static int TOOL_BUTTONS_V = 120;

    public ToolsWidget(PaintingScreen parentScreen, int x, int y) {
        // Add borders
        super(parentScreen, x, y, TOOL_BUTTON_WIDTH + 2, TOOL_BUTTON_HEIGHT * 5 + 2, Component.translatable("container.zetter.painting.tools"));

        this.buttons = new ArrayList<>() {{
            add(new ToolButton(Tool.PENCIL, TOOL_BUTTONS_U + TOOL_BUTTON_WIDTH + 3, TOOL_BUTTONS_V + 1, TOOL_BUTTON_WIDTH, TOOL_BUTTON_HEIGHT));
            add(new ToolButton(Tool.BRUSH, TOOL_BUTTONS_U + TOOL_BUTTON_WIDTH + 3, TOOL_BUTTONS_V + TOOL_BUTTON_HEIGHT + 1, TOOL_BUTTON_WIDTH, TOOL_BUTTON_HEIGHT));
            add(new ToolButton(Tool.EYEDROPPER, TOOL_BUTTONS_U + TOOL_BUTTON_WIDTH + 3, TOOL_BUTTONS_V + TOOL_BUTTON_HEIGHT * 2 + 1, TOOL_BUTTON_WIDTH, TOOL_BUTTON_HEIGHT));
            add(new ToolButton(Tool.BUCKET, TOOL_BUTTONS_U + TOOL_BUTTON_WIDTH + 3, TOOL_BUTTONS_V + TOOL_BUTTON_HEIGHT * 3 + 1, TOOL_BUTTON_WIDTH, TOOL_BUTTON_HEIGHT));
            add(new ToolButton(Tool.HAND, TOOL_BUTTONS_U + TOOL_BUTTON_WIDTH + 3, TOOL_BUTTONS_V + TOOL_BUTTON_HEIGHT * 4 + 1, TOOL_BUTTON_WIDTH, TOOL_BUTTON_HEIGHT));
        }};
    }

    @Override
    public @Nullable
    Component getTooltip(int mouseX, int mouseY) {
        int i = 0;
        for (ToolButton toolButton: this.buttons) {
            int fromY = this.getY() + 1 + i * TOOL_BUTTON_HEIGHT;

            if (isInRect(this.getX() + 1, fromY, TOOL_BUTTON_WIDTH, TOOL_BUTTON_HEIGHT, mouseX, mouseY)) {
                return toolButton.getTooltip();
            }

            i++;
        }

        return null;
    }

    @Override
    protected boolean clicked(double mouseX, double mouseY) {
        int iMouseX = (int) mouseX;
        int iMouseY = (int) mouseY;

        // Quick check
        if (!this.isMouseOver(mouseX, mouseY)) {
            return false;
        }

        int i = 0;
        for (ToolButton toolButton: this.buttons) {
            int fromY = this.getY() + 1 + i * TOOL_BUTTON_HEIGHT;

            if (isInRect(this.getX(), fromY, toolButton.width, toolButton.height, iMouseX, iMouseY)) {
                return true;
            }

            i++;
        }

        return false;
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        int iMouseX = (int) mouseX;
        int iMouseY = (int) mouseY;

        int i = 0;
        for (ToolButton toolButton: this.buttons) {
            int fromY = this.getY() + 1 + i * TOOL_BUTTON_HEIGHT;

            if (isInRect(this.getX(), fromY, toolButton.width, toolButton.height, iMouseX, iMouseY)) {
                this.updateCurrentTool(toolButton);
                return;
            }

            i++;
        }
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return false;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        narrationElementOutput.add(NarratedElementType.TITLE, this.createNarrationMessage());
    }

    @Override
    protected void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        guiGraphics.blit(PAINTING_WIDGETS_TEXTURE_RESOURCE,  this.getX(), this.getY(), TOOL_BUTTONS_U, TOOL_BUTTONS_V, TOOL_BUTTON_WIDTH + 2, TOOL_BUTTON_HEIGHT * this.buttons.size() + 2);

        int i = 0;
        for (ToolButton toolButton: this.buttons) {
            int fromY = this.getY() + 1 + i * TOOL_BUTTON_HEIGHT;

            if (toolButton.tool == Tool.HAND && this.parentScreen.getEaselState().canvasMode() != PaintingScreen.CanvasMode.OVERLAY) {
                i++;
                continue;
            }

            int uOffset = toolButton.uPosition + (this.parentScreen.getPaletteState().currentTool() == toolButton.tool ? TOOL_BUTTON_WIDTH + 2 : 0);

            guiGraphics.blit(PAINTING_WIDGETS_TEXTURE_RESOURCE,  this.getX() + 1, fromY, uOffset, toolButton.vPosition, toolButton.width, toolButton.height);
            i++;
        }
    }

    protected void updateCurrentTool(ToolButton toolButton) {
        this.parentScreen.setPaletteState(this.parentScreen.getPaletteState().withCurrentTool(toolButton.tool));
    }

    public class ToolButton {
        public final Tool tool;
        public final int uPosition;
        public final int vPosition;
        public final int height;
        public final int width;

        ToolButton(Tool tool, int uPosition, int vPosition, int width, int height) {
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
