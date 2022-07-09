package me.dantaeusb.zetter.painting.tools;

import me.dantaeusb.zetter.menu.EaselContainerMenu;
import me.dantaeusb.zetter.painting.parameters.AbstractToolParameters;
import me.dantaeusb.zetter.painting.pipes.Pipe;
import me.dantaeusb.zetter.storage.CanvasData;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.util.Tuple;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public abstract class AbstractTool<T extends AbstractToolParameters> {
    protected final List<Pipe> pipes;

    public AbstractTool(List<Pipe> pipes) {
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
     * @todo: do memo! cache shapes!!!
     */
    public abstract ToolShape getShape(T params);

    public static class ToolShape {
        // [0, 0], [0, 1], [1, 1], [1, 0] for square
        public final List<Tuple<Integer, Integer>> points;
        private ArrayList<ShapeLine> lines;

        public ToolShape() {
            this(new ArrayList<Tuple<Integer, Integer>>() {{
                add(new Tuple<>(0, 0));
                add(new Tuple<>(0, 1));
                add(new Tuple<>(1, 1));
                add(new Tuple<>(1, 0));
            }});
        }

        public ToolShape(ArrayList<Tuple<Integer, Integer>> points) {
            this.points = points;

            this.createLines();
        }

        private void createLines() {
            ArrayList<ShapeLine> lines = new ArrayList<>();

            for (int i = 0; i < this.points.size(); i++) {
                Tuple<Integer, Integer> pointA = this.points.get(i);
                Tuple<Integer, Integer> pointB = this.points.get(i + 1 != this.points.size() ? i + 1 : 0);

                int lengthX = pointB.getA() - pointA.getA();
                int lengthY = pointB.getB() - pointA.getB();

                if (lengthX != 0 && lengthY != 0) {
                    throw new IllegalStateException("Diagonal line cannot be used in brush shape");
                }

                if (lengthX > 0) {
                    lines.add(new ShapeLine(ShapeLine.LineDirection.HORIZONTAL, pointA.getA(), pointA.getB(), lengthX));
                } else if (lengthX < 0) {
                    lines.add(new ShapeLine(ShapeLine.LineDirection.HORIZONTAL, pointB.getA(), pointB.getB(), Math.abs(lengthX)));
                }

                if (lengthY > 0) {
                    lines.add(new ShapeLine(ShapeLine.LineDirection.VERTICAL, pointA.getA(), pointA.getB(), lengthY));
                } else if (lengthY < 0) {
                    lines.add(new ShapeLine(ShapeLine.LineDirection.VERTICAL, pointB.getA(), pointB.getB(), Math.abs(lengthY)));
                }
            }

            this.lines = lines;
        }

        public ArrayList<ShapeLine> getLines() {
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
