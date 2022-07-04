package me.dantaeusb.zetter.menu.painting.tools;

import me.dantaeusb.zetter.menu.EaselContainerMenu;
import me.dantaeusb.zetter.menu.painting.parameters.BrushParameters;
import me.dantaeusb.zetter.menu.painting.pipes.BlendingPipe;
import me.dantaeusb.zetter.menu.painting.pipes.DitheringPipe;
import me.dantaeusb.zetter.menu.painting.pipes.Pipe;
import me.dantaeusb.zetter.storage.CanvasData;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.util.Tuple;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class Brush extends AbstractTool<BrushParameters> {
    public static final String CODE = "brush";

    public static final int HOTKEY = GLFW.GLFW_KEY_B;

    private final TranslatableComponent translatableComponent = new TranslatableComponent("container.zetter.painting.tools.brush");

    public Brush(EaselContainerMenu menu) {
        super(Brush.CODE, menu, new ArrayList<>() {{
            add(new DitheringPipe());
            add(new BlendingPipe());
        }});
    }

    @Override
    public ToolShape getShape(BrushParameters params) {
        return null;
    }

    @Override
    public TranslatableComponent getTranslatableComponent() {
        return this.translatableComponent;
    }

    public boolean shouldAddAction(float newPosX, float newPosY, @Nullable Float lastPosX, @Nullable Float lastPosY) {
        if (lastPosX == null || lastPosY == null) {
            return true;
        }

        double distance = Math.sqrt(Math.pow(newPosX - lastPosX, 2) + Math.pow(newPosY - lastPosY, 2));

        return distance > 1d;
    }

    @Override
    public int apply(CanvasData canvasData, BrushParameters params, int color, float posX, float posY) {
        final int width = canvasData.getWidth();
        final int height = canvasData.getHeight();

        final int brushSize = 3;

        List<Tuple<Integer, Integer>> affectedPixels = Brush.getPixelsInDistance(canvasData, posX, posY, brushSize);

        for (Tuple<Integer, Integer> pixel : affectedPixels) {
            final double distanceToCenter = Math.sqrt(Math.pow(posX - (pixel.getA() + .5d), 2) + Math.pow(posY - (pixel.getB() + .5d), 2));
            final double proximity = Brush.clamp(1d - (distanceToCenter / brushSize), 0d, 1d); // 3d - size

            if (proximity == 0) {
                continue;
            }

            final int localIndex = pixel.getB() * width + pixel.getA();

            this.pixelChange(canvasData, params, color, localIndex, bezierIntensity(proximity));
        }

        return 1;
    }

    private static List<Tuple<Integer, Integer>> getPixelsInDistance(CanvasData canvasData, float posX, float posY, float size) {
        float radius = size / 2f;

        int minX = (int) Brush.clamp(Math.floor(posX - radius), 0d, canvasData.getWidth() - 1);
        int maxX = (int) Brush.clamp(Math.ceil(posX + radius), 0d, canvasData.getWidth() - 1);

        int minY = (int) Brush.clamp(Math.floor(posY - radius), 0d, canvasData.getHeight() - 1);
        int maxY = (int) Brush.clamp(Math.ceil(posY + radius), 0d, canvasData.getHeight() - 1);

        List<Tuple<Integer, Integer>> pixels = new LinkedList<>();

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                pixels.add(new Tuple<>(x, y));
            }
        }

        return pixels;
    }

    private static double clamp(double value, double min, double max) {
        return Math.min(Math.max(value, min), max);
    }

    // https://pomax.github.io/bezierinfo/#yforx
    // Quad cubic 0.455, 0.030, 0.515, 0.955 (CSS)
    private static float bezierIntensity(double distance) {
        final double a = 0.455;
        final double b = 0.030;
        final double c = 0.515;
        final double d = 0.955;

        double result = (-a + 3 * b - 3 * c + d) * Math.pow(distance, 3) +
                        (3 * a - 6 * b + 3 * c) * Math.pow(distance, 2) +
                        (-3 * a + 3 * b) * distance +
                        a;

        return (float) Math.max(Math.min(result, 1d), 0d);
    }
}
