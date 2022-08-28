package me.dantaeusb.zetter.painting.tools;

import me.dantaeusb.zetter.Zetter;
import me.dantaeusb.zetter.painting.parameters.AbstractToolParameters;
import me.dantaeusb.zetter.painting.pipes.Pipe;
import me.dantaeusb.zetter.storage.CanvasData;
import net.minecraft.network.chat.TranslatableComponent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractTool<T extends AbstractToolParameters> {
    protected final List<Pipe> pipes;

    protected final List<ActionListener> actionListeners = new ArrayList<>();

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
    public int apply(CanvasData canvas, T params, int color, float posX, float posY) {
        int result = this.useTool(canvas, params, color, posX, posY);
        this.applyListeners(canvas, params, color, posX, posY);

        return result;
    }

    protected abstract int useTool(CanvasData canvas, T params, int color, float posX, float posY);

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

    private void applyListeners(CanvasData canvas, T params, int color, float posX, float posY) {
        for (ActionListener actionListener : this.actionListeners) {
            actionListener.useToolCallback(canvas, this, params, color, posX, posY);
        }
    }

    public void addActionListener(ActionListener actionListener) {
        if (this.actionListeners.contains(actionListener)) {
            return;
        }

        this.actionListeners.add(actionListener);
    }

    public void removeActionListener(ActionListener actionListener) {
        if (!this.actionListeners.contains(actionListener)) {
            return;
        }

        this.actionListeners.remove(actionListener);
    }

    public static class ToolShape {
        protected final int[][] shape;
        private ArrayList<ShapeLine> lines;

        public ToolShape() {
            this(new int[][]{{1}});
        }

        public ToolShape(int[][] shape) {
            this.shape = shape;

            this.createLines();
        }

        /**
         * @todo: [LOW] Cringe, rewrite cleaner
         */
        private void createLines() {
            this.lines = new ArrayList<>();

            // x pass
            for (int y = 0; y < this.shape.length; y++) {
                int upperLineLength = 0;
                int lowerLineLength = 0;

                // Start before figure, end after
                for (int x = -1; x <= this.shape[y].length; x++) {
                    if (x >= 0 && x < this.shape[y].length && this.shape[y][x] == 1) {
                        // If there are no pixels above
                        if (y == 0 || this.shape[y - 1][x] == 0) {
                            upperLineLength++;
                        }

                        // If there are no pixels below
                        if (y == this.shape.length - 1 || this.shape[y + 1][x] == 0) {
                            lowerLineLength++;
                        }
                    } else {
                        if (upperLineLength > 0) {
                            this.commitLine(
                                ShapeLine.LineDirection.HORIZONTAL,
                                x,
                                y,
                                upperLineLength,
                                false
                            );

                            upperLineLength = 0;
                        }

                        if (lowerLineLength > 0) {
                            this.commitLine(
                                ShapeLine.LineDirection.HORIZONTAL,
                                x,
                                y,
                                lowerLineLength,
                                true
                            );

                            lowerLineLength = 0;
                        }
                    }
                }
            }

            // y pass
            for (int x = 0; x < this.shape.length; x++) {
                int leftLineLength = 0;
                int rightLineLength = 0;

                // Start before figure, end after
                for (int y = -1; y <= this.shape[x].length; y++) {
                    if (y >= 0 && y < this.shape.length && this.shape[y][x] == 1) {
                        // If there are no pixels before
                        if (x == 0 || this.shape[y][x - 1] == 0) {
                            leftLineLength++;
                        }

                        // If there are no pixels after
                        if (x == this.shape[y].length - 1 || this.shape[y][x + 1] == 0) {
                            rightLineLength++;
                        }
                    } else {
                        if (leftLineLength > 0) {
                            this.commitLine(
                                    ShapeLine.LineDirection.VERTICAL,
                                    x,
                                    y,
                                    leftLineLength,
                                    false
                            );

                            leftLineLength = 0;
                        }

                        if (rightLineLength > 0) {
                            this.commitLine(
                                    ShapeLine.LineDirection.VERTICAL,
                                    x,
                                    y,
                                    rightLineLength,
                                    true
                            );

                            rightLineLength = 0;
                        }
                    }
                }
            }

            return;
        }

        public ArrayList<ShapeLine> getLines() {
            return this.lines;
        }

        private void commitLine(ShapeLine.LineDirection direction, int x, int y, int length, boolean offset) {
            int xOffset = 0;
            int yOffset = 0;

            if (length == 0) {
                return;
            }

            if (direction == ShapeLine.LineDirection.HORIZONTAL) {
                if (offset) {
                    yOffset++;
                }

                xOffset -= length;
            }

            if (direction == ShapeLine.LineDirection.VERTICAL) {
                if (offset) {
                    xOffset++;
                }

                yOffset -= length;
            }

            this.lines.add(new ShapeLine(
                direction,
                x + xOffset,
                y + yOffset,
                length
            ));
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
