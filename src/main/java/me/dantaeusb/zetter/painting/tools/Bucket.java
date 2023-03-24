package me.dantaeusb.zetter.painting.tools;

import me.dantaeusb.zetter.painting.parameters.BucketParameters;
import me.dantaeusb.zetter.painting.pipes.Pipe;
import me.dantaeusb.zetter.storage.CanvasData;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.stream.Stream;

public class Bucket extends AbstractTool<BucketParameters> {
    public static final String CODE = "bucket";

    public static final int HOTKEY = GLFW.GLFW_KEY_F;

    private final Component translatableComponent = Component.translatable("container.zetter.painting.tools.bucket");

    private final ToolShape shape = new ToolShape();

    public Bucket() {
        super(new ArrayList<Pipe>());
    }

    @Override
    public ToolShape getShape(BucketParameters params) {
        return this.shape;
    }

    @Override
    public Component getTranslatableComponent() {
        return this.translatableComponent;
    }

    @Override
    public int useTool(CanvasData canvas, BucketParameters params, int color, float posX, float posY) {
        int position = canvas.getPixelIndex((int) posX, (int) posY);
        float opacity = params.getIntensity();

        final int width = canvas.getWidth();
        final int height = canvas.getHeight();
        final int length = width * height;
        final int replacedColor = canvas.getColorAt(position);

        ArrayDeque<Integer> positionsQueue = new ArrayDeque<>();
        Vector<Integer> checkedQueue = new Vector<>();
        Vector<Integer> paintQueue = new Vector<>();

        positionsQueue.add(position);
        paintQueue.add(position);

        do {
            getNeighborPositions(positionsQueue.pop(), width, length)
                    // Ignore checked positions if overlap
                    .filter(currentIndex -> !checkedQueue.contains(currentIndex))
                    .forEach(currentIndex -> {
                        if (canvas.getColorAt(currentIndex) == replacedColor) {
                            positionsQueue.add(currentIndex);
                            paintQueue.add(currentIndex);
                        }

                        checkedQueue.add(currentIndex);
                    });
        } while (!positionsQueue.isEmpty());

        for (int updateIndex: paintQueue) {
            this.pixelChange(canvas, params, color, updateIndex, 1f);
        }

        return Math.round(paintQueue.size() * opacity);
    }

    public static Stream<Integer> getNeighborPositions(int currentCenter, int width, int length) {
        List<Integer> neighborPositions = new ArrayList<>(4);

        final int topPosition = currentCenter - width;
        if (topPosition >= 0) {
            neighborPositions.add(topPosition);
        }

        final int leftPosition = currentCenter - 1;
        // on a single row
        if (leftPosition >= 0 && leftPosition / width == currentCenter / width) {
            neighborPositions.add(leftPosition);
        }

        final int rightPosition = currentCenter + 1;
        // on a single row
        if (rightPosition < length && rightPosition / width == currentCenter / width) {
            neighborPositions.add(rightPosition);
        }

        final int bottomPosition = currentCenter + width;
        if (bottomPosition < length) {
            neighborPositions.add(bottomPosition);
        }

        return neighborPositions.stream();
    }

}
