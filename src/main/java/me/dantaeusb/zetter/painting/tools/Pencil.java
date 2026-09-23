package me.dantaeusb.zetter.painting.tools;

import me.dantaeusb.zetter.painting.parameters.PencilParameters;
import me.dantaeusb.zetter.painting.pipes.BlendingPipe;
import me.dantaeusb.zetter.painting.pipes.DitheringPipe;
import me.dantaeusb.zetter.storage.CanvasData;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;

public class Pencil extends AbstractTool<PencilParameters> {
    public static final int HOTKEY = GLFW.GLFW_KEY_P;

    private final Component translatableComponent = Component.translatable("container.zetter.painting.tools.pencil");

    public Pencil() {
        super(new ArrayList<>() {{
            add(new DitheringPipe());
            add(new BlendingPipe());
        }});
    }

    @Override
    public ToolShape getShape(PencilParameters params) {
        return this.shapes.get(Math.round(params.getSize()));
    }

    @Override
    public Component getTranslatableComponent() {
        return this.translatableComponent;
    }

    @Override
    public boolean shouldAddAction(CanvasData canvasData, PencilParameters params, float newPosX, float newPosY, @Nullable Float lastPosX, @Nullable Float lastPosY) {
        ToolShape shape = this.getShape(params);
        final int distance = (int) Math.ceil(shape.getSize() / 2d);

        if (newPosX < -distance || newPosX > canvasData.getWidth() + distance) {
            return false;
        }

        if (newPosY < -distance || newPosY > canvasData.getHeight() + distance) {
            return false;
        }

        if (lastPosX == null || lastPosY == null) {
            return true;
        }

        return Math.floor(lastPosX) != Math.floor(newPosX) || Math.floor(lastPosY) != Math.floor(newPosY);
    }

    /**
     * Cursor positions arrive as far apart as the pointer moved between reports, so
     * the gap back to the point the stroke came from is filled with a line.
     *
     * Bresenham steps one cell at a time and takes the diagonal as a staircase,
     * which is the line a pixel art tool is expected to draw: covering every cell
     * the path grazes would round the stroke out and make it a pixel wider.
     *
     * @param canvas
     * @param params
     * @param color
     * @param posX
     * @param posY
     * @param lastPosX
     * @param lastPosY
     * @return
     */
    @Override
    public int apply(CanvasData canvas, PencilParameters params, int color, float posX, float posY, @Nullable Float lastPosX, @Nullable Float lastPosY) {
        if (lastPosX == null || lastPosY == null) {
            return this.useTool(canvas, params, color, posX, posY);
        }

        final int targetX = Mth.floor(posX);
        final int targetY = Mth.floor(posY);

        int currentX = Mth.floor(lastPosX);
        int currentY = Mth.floor(lastPosY);

        final int stepX = currentX < targetX ? 1 : -1;
        final int stepY = currentY < targetY ? 1 : -1;

        final int deltaX = Math.abs(targetX - currentX);
        final int deltaY = -Math.abs(targetY - currentY);

        int error = deltaX + deltaY;
        int damage = 0;

        // The cell the stroke came from was painted when that point was applied,
        // so stepping before painting leaves it alone instead of blending it twice
        while (currentX != targetX || currentY != targetY) {
            final int doubleError = error * 2;

            if (doubleError >= deltaY) {
                error += deltaY;
                currentX += stepX;
            }

            if (doubleError <= deltaX) {
                error += deltaX;
                currentY += stepY;
            }

            damage += this.useTool(canvas, params, color, currentX + 0.5f, currentY + 0.5f);
        }

        return damage;
    }

    @Override
    public int useTool(CanvasData canvas, PencilParameters params, int color, float posX, float posY) {
        final int width = canvas.getWidth();
        final int height = canvas.getHeight();

        final int canvasSize = canvas.getWidth() * canvas.getHeight();

        ToolShape shape = this.getShape(params);

        if (shape != null) {
            // Offset from the cursor center in canvas pixels
            final int offset = -(shape.getSize() / 2);

            int count = 0;

            for (int y = 0; y < shape.matrix.length; y++) {
                for (int x = 0; x < shape.matrix[y].length; x++) {
                    if (shape.matrix[y][x] == 0) {
                        continue;
                    }

                    final int canvasX = ((int) Math.floor(posX) + offset + x);
                    final int canvasY = ((int) Math.floor(posY) + offset + y);

                    if (canvasX < 0 || canvasX >= width) {
                        continue;
                    }

                    if (canvasY < 0 || canvasY >= height) {
                        continue;
                    }

                    final int index = canvasY * width + canvasX;

                    this.pixelChange(canvas, params, color, index, 1f);
                    count++;
                }
            }

            return count;
        } else {
            final int index = (int) (Math.floor(posY) * width + Math.floor(posX));
            this.pixelChange(canvas, params, color, index, 1f);

            return 1;
        }

    }

    private HashMap<Integer, ToolShape> shapes = new HashMap<>() {{
        put(1, new ToolShape());
        put(2, new ToolShape(new int[][]{
            {1, 1},
            {1, 1}
        }));
        put(3, new ToolShape(new int[][]{
            {0, 1, 0},
            {1, 1, 1},
            {0, 1, 0}
        }));
        put(4, new ToolShape(new int[][]{
            {1, 1, 1},
            {1, 1, 1},
            {1, 1, 1}
        }));
        put(5, new ToolShape(new int[][]{
            {0, 0, 1, 0, 0},
            {0, 1, 1, 1, 0},
            {1, 1, 1, 1, 1},
            {0, 1, 1, 1, 0},
            {0, 0, 1, 0, 0},
        }));
        put(6, new ToolShape(new int[][]{
            {0, 1, 1, 1, 0},
            {1, 1, 1, 1, 1},
            {1, 1, 1, 1, 1},
            {1, 1, 1, 1, 1},
            {0, 1, 1, 1, 0},
        }));
    }};
}
