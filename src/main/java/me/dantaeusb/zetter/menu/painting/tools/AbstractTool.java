package me.dantaeusb.zetter.menu.painting.tools;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.menu.EaselContainerMenu;
import me.dantaeusb.zetter.menu.painting.parameters.AbstractToolParameters;
import me.dantaeusb.zetter.menu.painting.parameters.BrushParameters;
import me.dantaeusb.zetter.menu.painting.pipes.Pipe;
import me.dantaeusb.zetter.storage.CanvasData;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.util.Tuple;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public abstract class AbstractTool<T extends AbstractToolParameters> {
    /**
     * Serializable code of the current tool
     */
    private final String code;

    /**
     * Link to the menu (container) for the unusual interactions
     * and side checks
     */
    protected final EaselContainerMenu menu;

    protected final List<Pipe> pipes;

    public AbstractTool(String code, EaselContainerMenu menu, List<Pipe> pipes) {
        this.code = code;
        this.menu = menu;
        this.pipes = pipes;
    }

    /**
     * Should be added to the action stack
     */
    public boolean publishable() {
        return true;
    }

    public boolean shouldAddAction(CanvasData canvasData, T parameters, float newPosX, float newPosY, @Nullable Float lastPosX, @Nullable Float lastPosY) {
        if (newPosX < 0 || newPosX > canvasData.getWidth()) {
            return false;
        }

        if (newPosY < 0 || newPosY > canvasData.getHeight()) {
            return false;
        }

        if (lastPosX == null || lastPosY == null) {
            return true;
        }

        return Math.floor(lastPosX) != Math.floor(newPosX) || Math.floor(lastPosY) != Math.floor(newPosY);
    }

    public abstract TranslatableComponent getTranslatableComponent();

    /**
     * Apply current tool to the canvas
     * Position is float for between-pixel coloring with brush
     *
     * Should be idempotent! Actions sent across network, not changes!
     * No random or side effects!
     *
     * Returns palette damage!
     */
    public abstract int apply(CanvasData canvas, T params, int color, float posX, float posY);

    /**
     * Do pixel change: apply all pipes and calculate final color
     *
     * @param canvas
     * @param parameters
     * @param color
     * @param index
     * @param localIntensity Local localIntensity! Used for brush smoothing, do not mix with blending localIntensity!
     */
    protected void pixelChange(CanvasData canvas, T parameters, int color, int index, float localIntensity) {
        if (!this.publishable()) {
            throw new IllegalStateException("Non-publishable tools cannot change pixels!");
        }

        if (index < 0 || index > canvas.getWidth() * canvas.getHeight()) {
            throw new IllegalStateException("Trying to update pixel outside of the canvas!");
        }

        for (Pipe pipe : this.pipes) {
            if (pipe.shouldUsePipe(this, parameters)) {
                color = pipe.applyPipe(canvas, parameters, color, index, localIntensity);
            }
        }

        canvas.updateCanvasPixel(index, color);
    }

    /**
     * Get cursor shape
     */
    public abstract ToolShape getShape(T params);

    public String getCode() {
        return this.code;
    }

    public static class ToolShape {
        // [0, 0], [0, 1], [1, 1], [1, 0] for square
        public final List<Tuple<Integer, Integer>> points;
        public Stack<ShapeLine> lines;

        public ToolShape() {
            this.points = new ArrayList<Tuple<Integer, Integer>>();
            this.points.add(new Tuple<>(0, 0));
            this.points.add(new Tuple<>(0, 1));
            this.points.add(new Tuple<>(1, 1));
            this.points.add(new Tuple<>(1, 0));

            this.createLines();
        }

        private void createLines() {
            Stack<ShapeLine> lines = new Stack<>();

            this.lines = lines;
        }

        public Stack<ShapeLine> getLines() {
            return this.lines;
        }
    }

    public record ShapeLine(AbstractTool.ShapeLine.LineDirection direction, int posX, int posY, int length) {
        public enum LineDirection {
            HORIZONTAL(0, "Horizontal"),
            VERTICAL(1, "Vertical");

            private final int step;
            private final String name;

            LineDirection(int step, String name) {
                this.step = step;
                this.name = name;
            }
        }
    }
}
