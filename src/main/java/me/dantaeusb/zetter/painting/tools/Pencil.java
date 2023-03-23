package me.dantaeusb.zetter.painting.tools;

import me.dantaeusb.zetter.painting.parameters.PencilParameters;
import me.dantaeusb.zetter.painting.pipes.BlendingPipe;
import me.dantaeusb.zetter.painting.pipes.DitheringPipe;
import me.dantaeusb.zetter.storage.CanvasData;
import net.minecraft.network.chat.Component;
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
