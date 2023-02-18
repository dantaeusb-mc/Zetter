package me.dantaeusb.zetter.painting.tools;

import me.dantaeusb.zetter.painting.parameters.BrushParameters;
import me.dantaeusb.zetter.painting.pipes.BlendingPipe;
import me.dantaeusb.zetter.painting.pipes.DitheringPipe;
import me.dantaeusb.zetter.painting.pipes.Pipe;
import me.dantaeusb.zetter.painting.tools.brush.Bezier;
import me.dantaeusb.zetter.storage.CanvasData;
import net.minecraft.util.Tuple;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class Brush extends AbstractTool<BrushParameters> {
    public static final int HOTKEY = GLFW.GLFW_KEY_B;

    private final ITextComponent translatableComponent = new TranslationTextComponent("container.zetter.painting.tools.brush");

    private final Bezier brushBezier = new Bezier(0.455, 0.030, 0.515, 0.955);

    public Brush() {
        super(new ArrayList<Pipe>() {{
            add(new DitheringPipe());
            add(new BlendingPipe());
        }});
    }

    @Override
    public ToolShape getShape(BrushParameters params) {
        return null;
    }

    @Override
    public ITextComponent getTranslatableComponent() {
        return this.translatableComponent;
    }

    @Override
    public boolean shouldAddAction(CanvasData canvasData, BrushParameters params, float newPosX, float newPosY, @Nullable Float lastPosX, @Nullable Float lastPosY) {
        int maxActiveDistance = (int) Math.ceil(params.getSize() / 2);

        if (newPosX < -maxActiveDistance || newPosX > canvasData.getWidth() + maxActiveDistance) {
            return false;
        }

        if (newPosY < -maxActiveDistance || newPosY > canvasData.getHeight() + maxActiveDistance) {
            return false;
        }

        if (lastPosX == null || lastPosY == null) {
            return true;
        }

        double distance = Math.sqrt(Math.pow(newPosX - lastPosX, 2) + Math.pow(newPosY - lastPosY, 2));

        return distance > 1d;
    }

    @Override
    public int useTool(CanvasData canvasData, BrushParameters params, int color, float posX, float posY) {
        final int width = canvasData.getWidth();
        final int height = canvasData.getHeight();

        final float intensity = params.getIntensity();

        final float brushSize = params.getSize();
        double totalDamage = 0;

        List<Tuple<Integer, Integer>> affectedPixels = Brush.getPixelsInDistance(canvasData, posX, posY, brushSize);

        for (Tuple<Integer, Integer> pixel : affectedPixels) {
            final double distanceToCenter = Math.sqrt(Math.pow(posX - (pixel.getA() + .5d), 2) + Math.pow(posY - (pixel.getB() + .5d), 2));
            final double proximity = Brush.clamp(1d - (distanceToCenter / brushSize), 0d, 1d); // 3d - size

            if (proximity == 0) {
                continue;
            }

            final int localIndex = pixel.getB() * width + pixel.getA();
            final double localIntensity = this.brushBezier.solve(proximity, 0.001d);

            this.pixelChange(canvasData, params, color, localIndex, (float) localIntensity);

            totalDamage += localIntensity;
        }

        return (int) Math.round(totalDamage * intensity);
    }

    private static List<Tuple<Integer, Integer>> getPixelsInDistance(CanvasData canvasData, float posX, float posY, float size) {
        float radius = size / 2f;

        int minX = (int) Brush.clamp(Math.floor(posX - radius), 0d, canvasData.getWidth() - 1);
        int maxX = (int) Brush.clamp(Math.ceil(posX + radius), 0d, canvasData.getWidth() - 1);

        int minY = (int) Brush.clamp(Math.floor(posY - radius), 0d, canvasData.getHeight() - 1);
        int maxY = (int) Brush.clamp(Math.ceil(posY + radius), 0d, canvasData.getHeight() - 1);

        List<Tuple<Integer, Integer>> pixels = new ArrayList<>();

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
}
