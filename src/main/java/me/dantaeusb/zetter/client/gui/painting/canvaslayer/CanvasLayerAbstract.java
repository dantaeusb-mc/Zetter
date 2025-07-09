package me.dantaeusb.zetter.client.gui.painting.canvaslayer;

import com.mojang.blaze3d.systems.RenderSystem;
import me.dantaeusb.zetter.client.gui.PaintingScreen;
import me.dantaeusb.zetter.painting.tools.AbstractTool;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;

import static org.lwjgl.opengl.GL14.GL_FUNC_ADD;
import static org.lwjgl.opengl.GL14.GL_FUNC_SUBTRACT;

public abstract class CanvasLayerAbstract implements GuiEventListener, NarratableEntry, Renderable {
    protected final PaintingScreen parentScreen;

    protected int leftPos;
    protected int topPos;

    protected int width;
    protected int height;

    protected int centerX;
    protected int centerY;

    protected boolean focused = false;
    protected boolean isHovered = false;

    public CanvasLayerAbstract(PaintingScreen parentScreen) {
        super();

        this.parentScreen = parentScreen;
    }

    public void init(int leftPos, int topPos, int width, int height) {
        this.leftPos = leftPos;
        this.topPos = topPos;
        this.width = width;
        this.height = height;

        this.centerX = leftPos + width / 2;
        this.centerY = topPos + height / 2;
    }

    @Override
    public void setFocused(boolean focused) {
        this.focused = focused;
    }

    @Override
    public boolean isFocused() {
        return this.focused;
    }

    public abstract void tick();

    protected abstract boolean handleCanvasInteraction(double mouseX, double mouseY, int button);

    protected boolean handleToolUse(int canvasX, int canvasY, int button) {
        this.parentScreen.useTool(canvasX, canvasY);
        return true;
    }

    protected void drawCursor(GuiGraphics guiGraphics, AbstractTool.ToolShape shape, int offsetX, int offsetY, int scale) {
        RenderSystem.blendEquation(GL_FUNC_SUBTRACT);

        //GLFW.glfwSetInputMode(Minecraft.getInstance().getWindow().getWindow(), GLFW_CURSOR, GLFW_CURSOR_NORMAL);

        for (AbstractTool.ShapeLine line : shape.getLines()) {
            // Relative positions from the cursor "center" in canvas pixels
            // Center is
            int posX = (line.posX()) * scale;
            int posY = (line.posY()) * scale;
            int length = line.length() * scale;

            if (line.direction() == AbstractTool.ShapeLine.LineDirection.HORIZONTAL) {
                // Wrap around top-left
                if (posX <= 0) {
                    posX--;

                    if (posX + length > 0) {
                        length++;
                    }
                }

                if (posY <= 0) {
                    posY--;
                }

                final int minX = offsetX + posX;
                guiGraphics.hLine(minX, minX + length, offsetY + posY, 0x80808080);
            } else {
                // Wrap around bottom-right
                if (posX <= 0) {
                    posX--;
                }

                if (posY <= 0) {
                    posY--;

                    if (posY + length > 0) {
                        length++;
                    }
                }

                final int minY = offsetY + posY;
                guiGraphics.vLine(offsetX + posX, minY, minY + length, 0x80808080);
            }
        }

        RenderSystem.blendEquation(GL_FUNC_ADD);
    }

    protected void drawCursor(GuiGraphics guiGraphics, int radius, int offsetX, int offsetY, int scale) {
        RenderSystem.blendEquation(GL_FUNC_SUBTRACT);

        //GLFW.glfwSetInputMode(Minecraft.getInstance().getWindow().getWindow(), GLFW_CURSOR, GLFW_CURSOR_HIDDEN);

        guiGraphics.hLine(offsetX - radius + 1, offsetX - 2, offsetY, 0x80808080);
        guiGraphics.hLine(offsetX + radius - 1, offsetX + 2, offsetY, 0x80808080);

        guiGraphics.vLine(offsetX, offsetY - radius, offsetY - 1, 0x80808080);
        guiGraphics.vLine(offsetX, offsetY + radius, offsetY + 1, 0x80808080);

        RenderSystem.blendEquation(GL_FUNC_ADD);
    }

    @Override
    public NarrationPriority narrationPriority() {
        if (this.isFocused()) {
            return NarratableEntry.NarrationPriority.FOCUSED;
        } else {
            return this.isHovered ? NarratableEntry.NarrationPriority.HOVERED : NarratableEntry.NarrationPriority.NONE;
        }
    }

    @Override
    public void updateNarration(NarrationElementOutput narrationElementOutput) {

    }
}
