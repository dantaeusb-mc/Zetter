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
    public int apply(CanvasData canvas, BrushParameters params, int color, float posX, float posY) {
        final int width = canvas.getWidth();
        final int height = canvas.getHeight();

        final int posXi = (int) Math.floor(posX);
        final int posYi = (int) Math.floor(posY);

        final int index = posYi * width + posXi;

        List<Tuple<Integer, Integer>> affectedPixels = new LinkedList<>();

        // @todo: it's not what should happen
        affectedPixels.add(new Tuple<>(posXi - 1, posYi - 1));
        affectedPixels.add(new Tuple<>(posXi, posYi - 1));
        affectedPixels.add(new Tuple<>(posXi + 1, posYi - 1));

        affectedPixels.add(new Tuple<>(posXi - 1, posYi));
        affectedPixels.add(new Tuple<>(posXi, posYi));
        affectedPixels.add(new Tuple<>(posXi + 1, posYi));

        affectedPixels.add(new Tuple<>(posXi - 1, posYi + 1));
        affectedPixels.add(new Tuple<>(posXi, posYi + 1));
        affectedPixels.add(new Tuple<>(posXi + 1, posYi + 1));

        for (Tuple<Integer, Integer> pixel : affectedPixels) {
            final double distanceToCenter = Math.sqrt(Math.pow(posX - (pixel.getA() + .5d), 2) + Math.pow(posY - (pixel.getB() + .5d), 2));
            final double proximity = Math.max(Math.min(1d - (distanceToCenter / 3d), 1d), 0d); // 3d - size

            if (proximity == 0) {
                continue;
            }

            final int localIndex = pixel.getB() * width + pixel.getA();

            this.pixelChange(canvas, params, color, localIndex, bezierIntensity(proximity));
        }

        return 1;
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
